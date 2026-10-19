import uuid

from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.auth.dependency import require_access
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import (
    Child, ChildDocument, DailyLog, Estancia, File, FilePurpose, Message, RecordStatus, Salon, User, UserRole,
)
from app.schemas.common import Detail
from app.schemas.org import ChildIn, ChildOut, ChildPhotoIn, ChildProfileIn
from app.storage.rules import load_attachable, release

router = APIRouter(prefix="/children", tags=["children"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail}}


def _visible_or_404(db: Session, access: Access, child_id: uuid.UUID) -> Child:
    child = lifecycle.get_or_404(db, Child, child_id, access)
    if not access.can_see_child(child.id):
        raise HTTPException(404, "Child not found")
    return child


@router.get("", response_model=list[ChildOut])
def list_children(salon_id: uuid.UUID | None = None, estancia_id: uuid.UUID | None = None,
                  parent_id: uuid.UUID | None = None, include_deleted: bool = False,
                  access: Access = Depends(require_access("child:read")), db: Session = Depends(get_db)):
    stmt = lifecycle.apply_visibility(select(Child).order_by(Child.full_name), Child, access, include_deleted)
    if not access.is_org:
        stmt = stmt.where(Child.id.in_(access.children))
    for column, value in ((Child.salon_id, salon_id), (Child.estancia_id, estancia_id), (Child.parent_id, parent_id)):
        if value:
            stmt = stmt.where(column == value)
    return db.scalars(stmt).all()


@router.get("/{child_id}", response_model=ChildOut, responses=ERRORS)
def get_child(child_id: uuid.UUID, access: Access = Depends(require_access("child:read")),
              db: Session = Depends(get_db)):
    return _visible_or_404(db, access, child_id)


@router.put("/{child_id}", response_model=ChildOut, responses={201: {"model": ChildOut}, **ERRORS})
def put_child(child_id: uuid.UUID, body: ChildIn, response: Response,
              access: Access = Depends(require_access("child:write")), db: Session = Depends(get_db)):
    """The estancia's ADMIN enrols a child, moves them between salones and links their parent."""
    child = db.get(Child, child_id)
    if child is None:
        if not access.can_see_estancia(body.estancia_id):
            raise HTTPException(404, "Estancia not found")
        estancia = lifecycle.get_or_404(db, Estancia, body.estancia_id, access)
        lifecycle.ensure_writable(estancia)
        child = Child(id=child_id, estancia_id=estancia.id)
        db.add(child)
        response.status_code = 201
    else:
        child = _visible_or_404(db, access, child_id)
        lifecycle.ensure_writable(child)
        if body.estancia_id != child.estancia_id:
            raise HTTPException(422, "A child can't move to another estancia")

    if body.salon_id is not None:
        salon = db.get(Salon, body.salon_id)
        if salon is None or salon.estancia_id != child.estancia_id or salon.status == RecordStatus.DELETED:
            raise HTTPException(422, "salon_id must be a salon of this estancia")
        if body.salon_id != child.salon_id:
            lifecycle.ensure_writable(salon)
    if body.parent_id is not None:
        parent = db.get(User, body.parent_id)
        if (parent is None or parent.status is not None or parent.role != UserRole.PADRE
                or parent.estancia_id != child.estancia_id):
            raise HTTPException(422, "parent_id must be an active PADRE of this estancia")
    child.salon_id, child.parent_id, child.full_name = body.salon_id, body.parent_id, body.full_name
    if "birth_date" in body.model_fields_set:  # left out = keep what the parent sent
        child.birth_date = body.birth_date
    if "allergies" in body.model_fields_set:
        child.allergies = ",".join(body.allergies or []) or None
    db.commit()
    return child


@router.patch("/{child_id}/profile", response_model=ChildOut, responses=ERRORS)
def complete_child_profile(child_id: uuid.UUID, body: ChildProfileIn,
                           access: Access = Depends(require_access("child:read")), db: Session = Depends(get_db)):
    """The child's parent fills in what the admin couldn't: birth date and allergies. Sending it marks the profile
    completed; it can be sent again to correct it. The name, salon and parent stay with the ADMIN."""
    if access.role != "PADRE":
        raise HTTPException(403, "Only the child's parent completes their profile")
    child = _visible_or_404(db, access, child_id)
    lifecycle.ensure_writable(child)
    child.birth_date = body.birth_date
    child.allergies = ",".join(body.allergies) or None
    if child.profile_completed_at is None:
        child.profile_completed_at = datetime.now(timezone.utc)
    db.commit()
    return child


@router.put("/{child_id}/photo", response_model=ChildOut, responses=ERRORS)
def put_child_photo(child_id: uuid.UUID, body: ChildPhotoIn,
                    access: Access = Depends(require_access("file:write")), db: Session = Depends(get_db)):
    """The child's parent sets (or removes) their profile photo with a file uploaded as CHILD_PHOTO."""
    if access.role != "PADRE":
        raise HTTPException(403, "Only the child's parent sets their photo")
    child = _visible_or_404(db, access, child_id)
    lifecycle.ensure_writable(child)
    if body.file_id != child.photo_file_id:
        if body.file_id is not None:
            load_attachable(db, body.file_id, FilePurpose.CHILD_PHOTO, child_id=child.id)
        if child.photo_file_id is not None:
            release(db.get(File, child.photo_file_id))
        child.photo_file_id = body.file_id
    db.commit()
    return child


@router.post("/{child_id}/archive", response_model=ChildOut, responses=ERRORS)
def archive_child(child_id: uuid.UUID, access: Access = Depends(require_access("child:delete")),
                  db: Session = Depends(get_db)):
    child = _visible_or_404(db, access, child_id)
    lifecycle.archive(child)
    db.commit()
    return child


@router.post("/{child_id}/unarchive", response_model=ChildOut, responses=ERRORS)
def unarchive_child(child_id: uuid.UUID, access: Access = Depends(require_access("child:delete")),
                    db: Session = Depends(get_db)):
    child = _visible_or_404(db, access, child_id)
    lifecycle.unarchive(child)
    db.commit()
    return child


@router.delete("/{child_id}", status_code=204, responses=ERRORS)
def delete_child(child_id: uuid.UUID, access: Access = Depends(require_access("child:delete")),
                 db: Session = Depends(get_db)):
    """Soft delete. Refused while the child still has active daily logs, messages or documents."""
    child = _visible_or_404(db, access, child_id)
    lifecycle.soft_delete(db, child, blockers=[
        (DailyLog, DailyLog.child_id), (Message, Message.child_id), (ChildDocument, ChildDocument.child_id)])
    db.commit()
    return Response(status_code=204)
