"""Who may upload what, and what a file may be attached to. Everything is decided from database rows
(the user's Access plus the target child / estancia), never from the object key."""
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.chat.members import find_chat, open_chat
from app.database.lifecycle import ensure_writable
from app.database.models import (
    ActivityImage, Child, ChildDocument, Estancia, File, FilePurpose, Message, RecordStatus, UploadState, User,
)

# purposes tied to a child -> roles that may upload them
CHILD_UPLOADERS = {
    FilePurpose.ACTIVITY_IMAGE: {"DOCENTE"},
    FilePurpose.CHILD_PHOTO: {"PADRE"},
    FilePurpose.CHILD_DOCUMENT: {"PADRE"},
}


@dataclass(frozen=True)
class UploadTarget:
    estancia_id: uuid.UUID | None
    child_id: uuid.UUID | None
    owner_id: uuid.UUID  # first folder of the object key
    peer_id: uuid.UUID | None = None


def authorize_upload(db: Session, access: Access, purpose: FilePurpose, target_id: uuid.UUID | None) -> UploadTarget:
    if purpose == FilePurpose.MESSAGE_ATTACHMENT:  # anyone who can write in the chat
        if target_id is None:
            raise HTTPException(422, "target_id (the chat) is required")
        chat = open_chat(db, access, target_id)
        if access.user_id not in chat.people:
            raise HTTPException(403, "You can read this chat but not write in it")
        ensure_writable(chat.record)
        if chat.child is not None:
            return UploadTarget(chat.estancia_id, chat.child.id, chat.child.id)
        return UploadTarget(chat.estancia_id, None, chat.peer.id, chat.peer.id)

    if purpose in CHILD_UPLOADERS:
        if access.role not in CHILD_UPLOADERS[purpose]:
            raise HTTPException(403, f"{access.role} can't upload {purpose.value}")
        if target_id is None:
            raise HTTPException(422, "target_id (the child) is required")
        child = db.get(Child, target_id)
        if child is None or not access.can_see_child(target_id) or child.status == RecordStatus.DELETED:
            raise HTTPException(404, "Child not found")
        ensure_writable(child)
        return UploadTarget(child.estancia_id, child.id, child.id)

    if purpose == FilePurpose.ESTANCIA_LOGO:
        if access.role != "ADMIN":
            raise HTTPException(403, "Only the estancia's ADMIN uploads its logo")
        if target_id is None:
            raise HTTPException(422, "target_id (the estancia) is required")
        estancia = db.get(Estancia, target_id)
        if estancia is None or target_id not in access.estancias:
            raise HTTPException(404, "Estancia not found")
        ensure_writable(estancia)
        return UploadTarget(estancia.id, None, estancia.id)

    # USER_AVATAR: always your own
    if target_id is not None and target_id != access.user_id:
        raise HTTPException(403, "You can only set your own avatar")
    estancia_id = next(iter(access.estancias), None)
    return UploadTarget(estancia_id, None, access.user_id)


def can_read_file(db: Session, access: Access, f: File) -> bool:
    if access.is_org:
        return True
    if f.status == RecordStatus.DELETED:
        return False
    if f.child_id is not None:
        return access.can_see_child(f.child_id)
    if f.peer_id is not None:  # a photo of a DOCENTE / ADMIN chat: only its people
        chat = find_chat(db, f.peer_id)
        return chat is not None and access.user_id in chat.people
    if f.purpose == FilePurpose.USER_AVATAR and f.uploaded_by == access.user_id:
        return True
    return f.estancia_id is not None and access.can_see_estancia(f.estancia_id)


def load_attachable(db: Session, file_id: uuid.UUID, purpose: FilePurpose, *, child_id: uuid.UUID | None = None,
                    estancia_id: uuid.UUID | None = None, peer_id: uuid.UUID | None = None,
                    uploader: uuid.UUID | None = None) -> File:
    """A file the caller may hang on a record: uploaded and checked, of the right purpose, for the right
    child / estancia / peer chat (and uploaded by `uploader`, when given)."""
    f = db.get(File, file_id)
    ok = (f is not None and f.status is None and f.upload_state == UploadState.READY and f.purpose == purpose
          and (child_id is None or f.child_id == child_id)
          and (estancia_id is None or f.estancia_id == estancia_id)
          and (peer_id is None or f.peer_id == peer_id)
          and (uploader is None or f.uploaded_by == uploader))
    if not ok:
        raise HTTPException(422, f"File {file_id} can't be used here")
    return f


def release(f: File | None) -> None:
    """The record that used this file let it go (replaced, removed). The object itself is kept."""
    if f is not None and f.status != RecordStatus.DELETED:
        f.status = RecordStatus.DELETED


def file_in_use(db: Session, file_id: uuid.UUID) -> bool:
    live = lambda m: m.status.is_distinct_from(RecordStatus.DELETED)
    checks = [
        select(ActivityImage.id).where(ActivityImage.file_id == file_id, live(ActivityImage)),
        select(Message.id).where(Message.attachment_file_id == file_id, live(Message)),
        select(ChildDocument.id).where(ChildDocument.file_id == file_id, live(ChildDocument)),
        select(User.id).where(User.avatar_file_id == file_id),
        select(Estancia.id).where(Estancia.logo_file_id == file_id),
        select(Child.id).where(Child.photo_file_id == file_id),
    ]
    return any(db.scalar(c.limit(1)) is not None for c in checks)


def expire_pending(db: Session, upload_ttl_seconds: int, limit: int = 200) -> int:
    """Uploads that were never confirmed become DELETED (recorded as done by the system)."""
    cutoff = datetime.now(timezone.utc) - timedelta(seconds=upload_ttl_seconds + 60)
    stale = db.scalars(select(File).where(
        File.upload_state == UploadState.PENDING, File.status.is_(None), File.created_at < cutoff).limit(limit)).all()
    if not stale:
        return 0
    actor = db.info.pop("actor_id", None)
    for f in stale:
        f.status = RecordStatus.DELETED
    db.commit()
    if actor is not None:
        db.info["actor_id"] = actor
    return len(stale)
