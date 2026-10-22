import logging
import uuid

from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.allergens.detector import THRESHOLD, AllergenRef, Meal
from app.allergens.names import is_no_allergy, normalize
from app.auth.access import Access
from app.auth.dependency import authenticated, require_access
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import Allergen, Child, Estancia
from app.rate_limit import rate_limit
from app.schemas.allergens import (
    AllergenConflict, AllergenOut, ChildConflict, DetectedAllergen, MenuCheckIn, MenuCheckOut,
)
from app.schemas.common import Detail

log = logging.getLogger("uvicorn.error")

router = APIRouter(tags=["allergens"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 502: {"model": Detail}, 503: {"model": Detail}}


def _split(allergies: str | None) -> list[str]:
    return [a.strip() for a in (allergies or "").split(",") if a.strip() and not is_no_allergy(a)]


@router.get("/allergens", response_model=list[AllergenOut])
def list_allergens(access: Access = Depends(authenticated()), db: Session = Depends(get_db)):
    """The catalog parents pick a child's allergies from."""
    return db.scalars(select(Allergen).order_by(Allergen.name)).all()


@router.post("/estancias/{estancia_id}/menu-check", response_model=MenuCheckOut, responses=ERRORS,
             dependencies=[Depends(rate_limit("menu-check", 10, 60))])
def menu_check(estancia_id: uuid.UUID, body: MenuCheckIn, request: Request,
               access: Access = Depends(require_access("estancia:read")), db: Session = Depends(get_db)):
    """The ADMIN sends the week's meals; Jev says which allergens each may contain, and the children of the
    estancia allergic to any of them come back, so special meals can be planned for them."""
    est = lifecycle.get_or_404(db, Estancia, estancia_id, access)
    if not access.can_see_estancia(est.id):
        raise HTTPException(404, "Estancia not found")
    if not access.is_org and access.role != "ADMIN":
        raise HTTPException(403, "Only the estancia's ADMIN checks the menu")
    detector = getattr(request.app.state, "allergen_detector", None)
    if detector is None:
        raise HTTPException(503, "Análisis de alergias no disponible")

    catalog = [AllergenRef(a.name, a.keywords) for a in db.scalars(select(Allergen).order_by(Allergen.name))]
    children = db.scalars(select(Child).options(selectinload(Child.salon))
                          .where(Child.estancia_id == est.id, lifecycle.active(Child))
                          .order_by(Child.full_name)).all()
    kids = [(c.id, c.full_name, c.salon.name if c.salon else None, _split(c.allergies)) for c in children]
    db.commit()  # done reading: don't hold a connection while Jev answers

    # Allergies typed before the catalog existed are checked too, with their own text as the hint
    known = {normalize(a.name) for a in catalog}
    extra = {normalize(a): a for _, _, _, items in kids for a in items if normalize(a) not in known}
    allergens = catalog + [AllergenRef(a, a) for a in extra.values()]

    meals = [Meal(m.name, m.ingredients) for m in body.meals]
    try:
        answers = detector.detect(meals, allergens)
    except Exception as e:  # Jev down, timeout, bad key: the admin can retry later
        log.warning("menu-check: allergen detection failed: %s", e)
        raise HTTPException(502, "No se pudo analizar el menú, intenta de nuevo")

    hits: dict[str, tuple[str, float, list[str]]] = {}  # normalized name -> (name, max probability, meals)
    for meal, probs in zip(meals, answers):
        for name, p in probs.items():
            if p >= THRESHOLD:
                _, best, in_meals = hits.get(normalize(name), (name, 0.0, []))
                hits[normalize(name)] = (name, max(best, p), [*in_meals, meal.name])

    conflicts = []
    for child_id, full_name, salon_name, items in kids:
        found = [AllergenConflict(allergen=a, meals=hits[normalize(a)][2]) for a in items if normalize(a) in hits]
        if found:
            conflicts.append(ChildConflict(child_id=child_id, full_name=full_name, salon_name=salon_name,
                                           conflicts=found))
    detected = sorted((DetectedAllergen(name=n, probability=round(p, 3), meals=ms) for n, p, ms in hits.values()),
                      key=lambda d: -d.probability)
    return MenuCheckOut(detected=detected, conflicts=conflicts)
