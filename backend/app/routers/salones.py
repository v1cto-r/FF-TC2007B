import uuid

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.auth.dependency import require_access
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import Child, Estancia, Notice, Salon, User, UserRole
from app.schemas.common import Detail
from app.schemas.org import SalonIn, SalonOut

router = APIRouter(prefix="/salones", tags=["salones"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail}}


def _visible_or_404(db: Session, access: Access, salon_id: uuid.UUID) -> Salon:
    salon = lifecycle.get_or_404(db, Salon, salon_id, access)
    if not access.can_see_salon(salon.id):
        raise HTTPException(404, "Salon not found")
    return salon


def _check_docente(db: Session, docente_id: uuid.UUID, estancia_id: uuid.UUID) -> None:
    docente = db.get(User, docente_id)
    if (docente is None or docente.status is not None or docente.role != UserRole.DOCENTE
            or docente.estancia_id != estancia_id):
        raise HTTPException(422, "docente_id must be an active DOCENTE of this estancia")


@router.get("", response_model=list[SalonOut])
def list_salones(estancia_id: uuid.UUID | None = None, include_deleted: bool = False,
                 access: Access = Depends(require_access("salon:read")), db: Session = Depends(get_db)):
    stmt = lifecycle.apply_visibility(select(Salon).order_by(Salon.name), Salon, access, include_deleted)
    if not access.is_org:
        stmt = stmt.where(Salon.id.in_(access.salones))
    if estancia_id:
        stmt = stmt.where(Salon.estancia_id == estancia_id)
    return db.scalars(stmt).all()


@router.get("/{salon_id}", response_model=SalonOut, responses=ERRORS)
def get_salon(salon_id: uuid.UUID, access: Access = Depends(require_access("salon:read")),
              db: Session = Depends(get_db)):
    return _visible_or_404(db, access, salon_id)


@router.put("/{salon_id}", response_model=SalonOut, responses={201: {"model": SalonOut}, **ERRORS})
def put_salon(salon_id: uuid.UUID, body: SalonIn, response: Response,
              access: Access = Depends(require_access("salon:write")), db: Session = Depends(get_db)):
    """The estancia's ADMIN creates salones, renames them and assigns their docente."""
    salon = db.get(Salon, salon_id)
    if salon is None:
        if not access.can_see_estancia(body.estancia_id):
            raise HTTPException(404, "Estancia not found")
        estancia = lifecycle.get_or_404(db, Estancia, body.estancia_id, access)
        lifecycle.ensure_writable(estancia)
        salon = Salon(id=salon_id, estancia_id=estancia.id)
        db.add(salon)
        response.status_code = 201
    else:
        salon = _visible_or_404(db, access, salon_id)
        lifecycle.ensure_writable(salon)
        if body.estancia_id != salon.estancia_id:
            raise HTTPException(422, "A salon can't move to another estancia")
    if body.docente_id is not None:
        _check_docente(db, body.docente_id, salon.estancia_id)
    salon.name, salon.group_type, salon.docente_id = body.name, body.group_type, body.docente_id
    db.commit()
    return salon


@router.post("/{salon_id}/archive", response_model=SalonOut, responses=ERRORS)
def archive_salon(salon_id: uuid.UUID, access: Access = Depends(require_access("salon:delete")),
                  db: Session = Depends(get_db)):
    salon = _visible_or_404(db, access, salon_id)
    lifecycle.archive(salon)
    db.commit()
    return salon


@router.post("/{salon_id}/unarchive", response_model=SalonOut, responses=ERRORS)
def unarchive_salon(salon_id: uuid.UUID, access: Access = Depends(require_access("salon:delete")),
                    db: Session = Depends(get_db)):
    salon = _visible_or_404(db, access, salon_id)
    lifecycle.unarchive(salon)
    db.commit()
    return salon


@router.delete("/{salon_id}", status_code=204, responses=ERRORS)
def delete_salon(salon_id: uuid.UUID, access: Access = Depends(require_access("salon:delete")),
                 db: Session = Depends(get_db)):
    """Soft delete. Refused while the salon still has active children or notices."""
    salon = _visible_or_404(db, access, salon_id)
    lifecycle.soft_delete(db, salon, blockers=[(Child, Child.salon_id), (Notice, Notice.salon_id)])
    db.commit()
    return Response(status_code=204)
