"""Chat rules shared by the socket (sending) and the REST routes (history, delete). Who is in which chat: members.py."""
import os
import uuid

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.chat.members import open_chat
from app.database import lifecycle
from app.database.models import File, FilePurpose, Message
from app.schemas.comms import MessageIn, MessageOut
from app.storage.rules import load_attachable


def visible_message(db: Session, access: Access, message_id: uuid.UUID) -> Message:
    msg = lifecycle.get_or_404(db, Message, message_id, access)
    try:
        open_chat(db, access, msg.chat_id)
    except HTTPException:
        raise HTTPException(404, "Message not found")
    return msg


def send_message(db: Session, access: Access, message_id: uuid.UUID, body: MessageIn) -> tuple[Message, bool]:
    """Stores a message and returns `(message, created)`. Messages are never edited: sending the same message again
    (an offline retry) returns the stored one with created=False, a different body for the same id is a 409."""
    if db.get(Message, message_id) is not None:
        existing = visible_message(db, access, message_id)
        same = (existing.author_id == access.user_id and existing.chat_id == body.chat_id
                and existing.content == body.content and existing.sent_at == body.sent_at
                and existing.is_aviso == body.is_aviso and existing.attachment_file_id == body.attachment_file_id)
        if not same:
            raise HTTPException(409, "Messages can't be edited")
        return existing, False

    chat = open_chat(db, access, body.chat_id)
    if access.user_id not in chat.people:  # ORG reads the child and docente chats but doesn't write in them
        raise HTTPException(403, "You can read this chat but not write in it")
    lifecycle.ensure_writable(chat.record)
    child_id = chat.child.id if chat.child else None
    peer_id = chat.peer.id if chat.peer else None
    if body.attachment_file_id is not None:
        load_attachable(db, body.attachment_file_id, FilePurpose.MESSAGE_ATTACHMENT, child_id=child_id,
                        peer_id=peer_id, uploader=access.user_id)
    msg = Message(id=message_id, child_id=child_id, peer_id=peer_id, author_id=access.user_id, content=body.content,
                  sent_at=body.sent_at, is_aviso=body.is_aviso, attachment_file_id=body.attachment_file_id)
    db.add(msg)
    db.commit()
    return msg, True


def render(db: Session, messages: list[Message]) -> list[MessageOut]:
    """Wire form of messages. Markers are appended as last lines, in this order: `::Aviso` when the message is an
    aviso, then `::Image /<file id>.<ext>` for a photo (the client fetches `GET /files/<file id>` for the signed URL).
    The client strips them. The stored `content` stays the plain text."""
    ids = {m.attachment_file_id for m in messages if m.attachment_file_id}
    keys = dict(db.execute(select(File.id, File.key).where(File.id.in_(ids))).all()) if ids else {}
    out = []
    for m in messages:
        item = MessageOut.model_validate(m)
        lines = [m.content] if m.content else []
        if m.is_aviso:
            lines.append("::Aviso")
        if m.attachment_file_id:
            lines.append(f"::Image /{m.attachment_file_id}{os.path.splitext(keys.get(m.attachment_file_id, ''))[1]}")
        item.content = "\n".join(lines)
        out.append(item)
    return out
