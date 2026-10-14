"""Chat rules shared by the socket (sending) and the REST routes (history, delete)."""
import os
import uuid

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.database import lifecycle
from app.database.models import Child, Estancia, File, FilePurpose, Message, Salon
from app.schemas.comms import MessageIn, MessageOut
from app.storage.rules import load_attachable


def visible_message(db: Session, access: Access, message_id: uuid.UUID) -> Message:
    msg = lifecycle.get_or_404(db, Message, message_id, access)
    if not access.can_see_child(msg.child_id):
        raise HTTPException(404, "Message not found")
    return msg


def send_message(db: Session, access: Access, message_id: uuid.UUID, body: MessageIn) -> tuple[Message, bool]:
    """Stores a message and returns `(message, created)`. Messages are never edited: sending the same message again
    (an offline retry) returns the stored one with created=False, a different body for the same id is a 409."""
    if access.role not in ("PADRE", "DOCENTE"):
        raise HTTPException(403, f"{access.role} can't send messages")
    if db.get(Message, message_id) is not None:
        existing = visible_message(db, access, message_id)
        same = (existing.author_id == access.user_id and existing.child_id == body.child_id
                and existing.content == body.content and existing.sent_at == body.sent_at
                and existing.is_aviso == body.is_aviso and existing.attachment_file_id == body.attachment_file_id)
        if not same:
            raise HTTPException(409, "Messages can't be edited")
        return existing, False

    if not access.can_see_child(body.child_id):
        raise HTTPException(404, "Child not found")
    child = lifecycle.get_or_404(db, Child, body.child_id, access)
    lifecycle.ensure_writable(child)
    if body.attachment_file_id is not None:
        load_attachable(db, body.attachment_file_id, FilePurpose.MESSAGE_ATTACHMENT, child_id=child.id,
                        uploader=access.user_id)
    msg = Message(id=message_id, child_id=child.id, author_id=access.user_id, content=body.content,
                  sent_at=body.sent_at, is_aviso=body.is_aviso, attachment_file_id=body.attachment_file_id)
    db.add(msg)
    db.commit()
    return msg, True


def recipients(db: Session, child_id: uuid.UUID) -> set[uuid.UUID]:
    """Who takes part in a child's chat: their parent, the docente of their salon and the admin of their estancia.
    ORG can read every chat through the REST history but isn't a live participant."""
    row = db.execute(
        select(Child.parent_id, Salon.docente_id, Estancia.admin_user_id)
        .join(Salon, Salon.id == Child.salon_id, isouter=True)
        .join(Estancia, Estancia.id == Child.estancia_id, isouter=True)
        .where(Child.id == child_id)).first()
    return {u for u in row or () if u is not None}


def render(db: Session, messages: list[Message]) -> list[MessageOut]:
    """Wire form of messages. A photo shows up as a last line `::Image /<file id>.<ext>`: the client strips it and
    fetches `GET /files/<file id>` for the signed URL. The stored `content` stays the plain text."""
    ids = {m.attachment_file_id for m in messages if m.attachment_file_id}
    keys = dict(db.execute(select(File.id, File.key).where(File.id.in_(ids))).all()) if ids else {}
    out = []
    for m in messages:
        item = MessageOut.model_validate(m)
        if m.attachment_file_id:
            marker = f"::Image /{m.attachment_file_id}{os.path.splitext(keys.get(m.attachment_file_id, ''))[1]}"
            item.content = f"{m.content}\n{marker}" if m.content else marker
        out.append(item)
    return out
