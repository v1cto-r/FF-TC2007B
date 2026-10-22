import uuid

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.auth.dependency import require_access
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import Child, Estancia, File, FilePurpose, Notice, Salon, User, UserRole
from app.schemas.common import Detail
from app.schemas.org import EstanciaIn, EstanciaOut
from app.storage.rules import load_attachable, release

router = APIRouter(prefix="/estancias", tags=["estancias"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}}


def _visible_or_404(db: Session, access: Access, estancia_id: uuid.UUID) -> Estancia:
    est = lifecycle.get_or_404(db, Estancia, estancia_id, access)
    if not access.can_see_estancia(est.id):
        raise HTTPException(404, "Estancia not found")
    return est


def _check_admin(db: Session, admin_user_id: uuid.UUID, estancia_id: uuid.UUID) -> None:
    """The user must be a live ADMIN who doesn't already run another live estancia."""
    admin = db.get(User, admin_user_id)
    if admin is None or admin.status is not None or admin.role != UserRole.ADMIN:
        raise HTTPException(422, "admin_user_id must be an active ADMIN user")
    taken = db.scalar(select(Estancia.id).where(
        Estancia.admin_user_id == admin_user_id, Estancia.id != estancia_id, lifecycle.live(Estancia)))
    if taken:
        raise HTTPException(409, "That ADMIN already runs another estancia")


@router.get("", response_model=list[EstanciaOut])
def list_estancias(include_deleted: bool = False, access: Access = Depends(require_access("estancia:read")),
                   db: Session = Depends(get_db)):
    stmt = lifecycle.apply_visibility(select(Estancia).order_by(Estancia.name), Estancia, access, include_deleted)
    if not access.is_org:
        stmt = stmt.where(Estancia.id.in_(access.estancias))
    return db.scalars(stmt).all()


@router.get("/{estancia_id}", response_model=EstanciaOut, responses=ERRORS)
def get_estancia(estancia_id: uuid.UUID, access: Access = Depends(require_access("estancia:read")),
                 db: Session = Depends(get_db)):
    return _visible_or_404(db, access, estancia_id)


@router.put("/{estancia_id}", response_model=EstanciaOut, responses={201: {"model": EstanciaOut}, **ERRORS})
def put_estancia(estancia_id: uuid.UUID, body: EstanciaIn, response: Response,
                 access: Access = Depends(require_access("estancia:write")), db: Session = Depends(get_db)):
    """ORG creates estancias and assigns their ADMIN. An ADMIN edits its own estancia (name, address, logo)."""
    est = db.get(Estancia, estancia_id)
    if est is None:
        if not access.is_org:
            raise HTTPException(403, "Only ORG creates estancias")
        if body.admin_user_id is None:
            raise HTTPException(422, "admin_user_id is required")
        _check_admin(db, body.admin_user_id, estancia_id)
        est = Estancia(id=estancia_id, name=body.name, address=body.address, admin_user_id=body.admin_user_id)
        db.add(est)
        db.flush()
        response.status_code = 201
    else:
        est = _visible_or_404(db, access, estancia_id)
        lifecycle.ensure_writable(est)
        if body.admin_user_id is not None and body.admin_user_id != est.admin_user_id:
            if not access.is_org:
                raise HTTPException(403, "Only ORG changes the admin")
            _check_admin(db, body.admin_user_id, est.id)
            est.admin_user_id = body.admin_user_id
        est.name, est.address = body.name, body.address

    if "logo_file_id" in body.model_fields_set and body.logo_file_id != est.logo_file_id:  # omitted = keep
        if body.logo_file_id is not None:
            load_attachable(db, body.logo_file_id, FilePurpose.ESTANCIA_LOGO, estancia_id=est.id)
        if est.logo_file_id is not None:
            release(db.get(File, est.logo_file_id))
        est.logo_file_id = body.logo_file_id
    db.commit()
    return est


@router.post("/{estancia_id}/archive", response_model=EstanciaOut, responses=ERRORS)
def archive_estancia(estancia_id: uuid.UUID, access: Access = Depends(require_access("estancia:delete")),
                     db: Session = Depends(get_db)):
    est = _visible_or_404(db, access, estancia_id)
    lifecycle.archive(est)
    db.commit()
    return est


@router.post("/{estancia_id}/unarchive", response_model=EstanciaOut, responses=ERRORS)
def unarchive_estancia(estancia_id: uuid.UUID, access: Access = Depends(require_access("estancia:delete")),
                       db: Session = Depends(get_db)):
    est = _visible_or_404(db, access, estancia_id)
    lifecycle.unarchive(est)
    db.commit()
    return est


@router.delete("/{estancia_id}", status_code=204, responses=ERRORS)
def delete_estancia(estancia_id: uuid.UUID, access: Access = Depends(require_access("estancia:delete")),
                    db: Session = Depends(get_db)):
    """Soft delete. Refused while the estancia still has active salones, children, notices or staff / parents."""
    est = _visible_or_404(db, access, estancia_id)
    lifecycle.soft_delete(db, est, blockers=[
        (Salon, Salon.estancia_id), (Child, Child.estancia_id), (Notice, Notice.estancia_id),
        (User, User.estancia_id)])
    db.commit()
    return Response(status_code=204)
