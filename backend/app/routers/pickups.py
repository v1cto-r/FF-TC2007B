import uuid
from datetime import date, timedelta

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app import pickups
from app.auth.access import Access
from app.auth.dependency import require_access
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import Child, PickupAuthorization, PickupKind, RecordStatus
from app.schemas.common import Detail
from app.schemas.pickups import PickupIn, PickupOut

router = APIRouter(prefix="/pickup-authorizations", tags=["pickup-authorizations"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail}}


def _out(p: PickupAuthorization) -> PickupOut:
    return PickupOut(id=p.id, status=p.status, created_at=p.created_at, updated_at=p.updated_at,
                     child_id=p.child_id, created_by=p.created_by, kind=p.kind, start_date=p.start_date,
                     weekdays=pickups.to_weekdays(p.weekdays), authorized_person_name=p.authorized_person_name,
                     relationship=p.relationship, phone=p.phone, identification_type=p.identification_type,
                     notes=p.notes)


def _visible_or_404(db: Session, access: Access, pickup_id: uuid.UUID) -> PickupAuthorization:
    p = lifecycle.get_or_404(db, PickupAuthorization, pickup_id, access)
    if not access.can_see_child(p.child_id):
        raise HTTPException(404, "PickupAuthorization not found")
    return p


def _ensure_parent(access: Access, child_id: uuid.UUID) -> None:
    if access.role != "PADRE":
        raise HTTPException(403, "Only the child's parent manages pickup authorizations")
    if not access.can_see_child(child_id):
        raise HTTPException(404, "Child not found")


@router.get("", response_model=list[PickupOut])
def list_pickups(child_id: uuid.UUID | None = None, on_date: date | None = None, include_deleted: bool = False,
                 access: Access = Depends(require_access("pickup:read")), db: Session = Depends(get_db)):
    """The authorizations of the children you can see. THIS_WEEK ones of a past week are left out.
    With `on_date`: only the one that applies that day, per child."""
    day = on_date or pickups.local_today()
    stmt = lifecycle.apply_visibility(
        select(PickupAuthorization).order_by(PickupAuthorization.created_at), PickupAuthorization, access,
        include_deleted)
    stmt = stmt.where(or_(PickupAuthorization.kind == PickupKind.RECURRING,
                          PickupAuthorization.start_date >= pickups.week_start(day)))
    if not access.is_org:
        stmt = stmt.where(PickupAuthorization.child_id.in_(access.children))
    if child_id:
        stmt = stmt.where(PickupAuthorization.child_id == child_id)
    rows = list(db.scalars(stmt))
    if on_date is None:
        return [_out(p) for p in rows]
    by_child: dict[uuid.UUID, list[PickupAuthorization]] = {}
    for p in rows:
        if p.status != RecordStatus.DELETED:
            by_child.setdefault(p.child_id, []).append(p)
    return [_out(p) for p in (pickups.resolve(ps, on_date) for ps in by_child.values()) if p is not None]


@router.get("/{pickup_id}", response_model=PickupOut, responses=ERRORS)
def get_pickup(pickup_id: uuid.UUID, access: Access = Depends(require_access("pickup:read")),
               db: Session = Depends(get_db)):
    return _out(_visible_or_404(db, access, pickup_id))


@router.put("/{pickup_id}", response_model=PickupOut, responses={201: {"model": PickupOut}, **ERRORS})
def put_pickup(pickup_id: uuid.UUID, body: PickupIn, response: Response, replace: bool = False,
               access: Access = Depends(require_access("pickup:write")), db: Session = Depends(get_db)):
    """Creates or updates an authorization of your own child. One that would apply on a day another one
    already covers is a 409, unless `replace=true`: then the other ones are cancelled in the same step.
    A THIS_WEEK authorization can't add days of this week that already passed."""
    _ensure_parent(access, body.child_id)
    # Locks the child: two writes for the same child can't both pass the overlap check
    child = db.scalar(select(Child).where(Child.id == body.child_id).with_for_update())
    if child is None or child.status == RecordStatus.DELETED:
        raise HTTPException(404, "Child not found")
    lifecycle.ensure_writable(child)

    today = pickups.local_today()
    p = db.get(PickupAuthorization, pickup_id)
    if p is None:
        start = pickups.week_start(today) if body.kind == PickupKind.THIS_WEEK else today
        old_mask = 0
    else:
        p = _visible_or_404(db, access, pickup_id)
        lifecycle.ensure_writable(p)
        if body.child_id != p.child_id:
            raise HTTPException(422, "An authorization can't move to another child")
        if body.kind != p.kind:
            raise HTTPException(422, "An authorization can't change its kind; cancel it and create another")
        if p.kind == PickupKind.THIS_WEEK and p.start_date < pickups.week_start(today):
            raise HTTPException(409, "That week is over; create a new authorization")
        start, old_mask = p.start_date, p.weekdays

    mask = pickups.to_mask(body.weekdays)
    if body.kind == PickupKind.THIS_WEEK:
        added = pickups.to_weekdays(mask & ~old_mask)
        if any(start + timedelta(days=d - 1) < today for d in added):
            raise HTTPException(422, "Days of this week that already passed can't be added")

    others = db.scalars(select(PickupAuthorization).where(
        PickupAuthorization.child_id == child.id, PickupAuthorization.id != pickup_id,
        lifecycle.active(PickupAuthorization)))
    clashing = [o for o in others if pickups.conflicts(body.kind, start, mask, o)]
    if clashing and not replace:
        raise HTTPException(409, "Overlaps the pickup authorization " + ", ".join(str(o.id) for o in clashing))
    for o in clashing:
        lifecycle.soft_delete(db, o)

    if p is None:
        p = PickupAuthorization(id=pickup_id, child_id=child.id, created_by=access.user_id, kind=body.kind,
                                start_date=start)
        db.add(p)
        response.status_code = 201
    p.weekdays = mask
    p.authorized_person_name, p.relationship = body.authorized_person_name, body.relationship
    p.phone, p.identification_type, p.notes = body.phone, body.identification_type, body.notes
    db.commit()
    return _out(p)


@router.delete("/{pickup_id}", status_code=204, responses=ERRORS)
def cancel_pickup(pickup_id: uuid.UUID, access: Access = Depends(require_access("pickup:delete")),
                  db: Session = Depends(get_db)):
    """The parent cancels it. Check-outs that already used it keep pointing at it."""
    p = _visible_or_404(db, access, pickup_id)
    _ensure_parent(access, p.child_id)
    lifecycle.soft_delete(db, p)
    db.commit()
    return Response(status_code=204)
