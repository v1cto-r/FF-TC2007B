import uuid

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.auth.dependency import require_access
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import Estancia, Notice, RecordStatus, Salon
from app.schemas.comms import NoticeIn, NoticeOut
from app.schemas.common import Detail

router = APIRouter(prefix="/notices", tags=["notices"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail}}


def _visible_or_404(db: Session, access: Access, notice_id: uuid.UUID) -> Notice:
    notice = lifecycle.get_or_404(db, Notice, notice_id, access)
    seen = access.can_see_estancia(notice.estancia_id) and (notice.salon_id is None or access.can_see_salon(notice.salon_id))
    if not seen:
        raise HTTPException(404, "Notice not found")
    return notice


def _can_modify(access: Access, notice: Notice) -> bool:
    """An ADMIN of the estancia, or the notice's own author."""
    return (access.role == "ADMIN" and notice.estancia_id in access.estancias) or notice.author_id == access.user_id


@router.get("", response_model=list[NoticeOut])
def list_notices(estancia_id: uuid.UUID | None = None, salon_id: uuid.UUID | None = None,
                 include_deleted: bool = False, access: Access = Depends(require_access("notice:read")),
                 db: Session = Depends(get_db)):
    """Estancia-wide notices plus those of the salones you can see."""
    stmt = lifecycle.apply_visibility(select(Notice).order_by(Notice.date.desc(), Notice.created_at.desc()),
                                      Notice, access, include_deleted)
    if not access.is_org:
        stmt = stmt.where(Notice.estancia_id.in_(access.estancias),
                          or_(Notice.salon_id.is_(None), Notice.salon_id.in_(access.salones)))
    if estancia_id:
        stmt = stmt.where(Notice.estancia_id == estancia_id)
    if salon_id:
        stmt = stmt.where(Notice.salon_id == salon_id)
    return db.scalars(stmt).all()


@router.get("/{notice_id}", response_model=NoticeOut, responses=ERRORS)
def get_notice(notice_id: uuid.UUID, access: Access = Depends(require_access("notice:read")),
               db: Session = Depends(get_db)):
    return _visible_or_404(db, access, notice_id)


@router.put("/{notice_id}", response_model=NoticeOut, responses={201: {"model": NoticeOut}, **ERRORS})
def put_notice(notice_id: uuid.UUID, body: NoticeIn, response: Response,
               access: Access = Depends(require_access("notice:write")), db: Session = Depends(get_db)):
    """An ADMIN posts to the whole estancia or to one salon; a DOCENTE only to their own salones.
    Only the author or the estancia's ADMIN can edit. estancia and salon can't change afterwards."""
    notice = db.get(Notice, notice_id)
    if notice is None:
        if access.role not in ("ADMIN", "DOCENTE"):
            raise HTTPException(403, f"{access.role} can't post notices")
        if body.estancia_id not in access.estancias:
            raise HTTPException(404, "Estancia not found")
        lifecycle.ensure_writable(lifecycle.get_or_404(db, Estancia, body.estancia_id, access))
        if body.salon_id is None:
            if access.role != "ADMIN":
                raise HTTPException(403, "Only an ADMIN posts to the whole estancia")
        else:
            salon = db.get(Salon, body.salon_id)
            if (salon is None or salon.estancia_id != body.estancia_id or salon.status == RecordStatus.DELETED
                    or body.salon_id not in access.salones):
                raise HTTPException(404 if access.role == "DOCENTE" else 422, "Salon not found in this estancia")
            lifecycle.ensure_writable(salon)
        notice = Notice(id=notice_id, estancia_id=body.estancia_id, salon_id=body.salon_id, author_id=access.user_id)
        db.add(notice)
        response.status_code = 201
    else:
        notice = _visible_or_404(db, access, notice_id)
        lifecycle.ensure_writable(notice)
        if not _can_modify(access, notice):
            raise HTTPException(403, "Only the author or the estancia's ADMIN can edit this notice")
        if (body.estancia_id, body.salon_id) != (notice.estancia_id, notice.salon_id):
            raise HTTPException(422, "A notice can't move to another estancia or salon")
    notice.name, notice.description, notice.date = body.name, body.description, body.date
    db.commit()
    return notice


@router.delete("/{notice_id}", status_code=204, responses=ERRORS)
def delete_notice(notice_id: uuid.UUID, access: Access = Depends(require_access("notice:delete")),
                  db: Session = Depends(get_db)):
    notice = _visible_or_404(db, access, notice_id)
    if not _can_modify(access, notice):
        raise HTTPException(403, "Only the author or the estancia's ADMIN can delete this notice")
    lifecycle.soft_delete(db, notice)
    db.commit()
    return Response(status_code=204)
