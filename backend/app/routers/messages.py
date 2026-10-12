import uuid

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.auth.dependency import require_access
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import Child, File, FilePurpose, Message
from app.schemas.comms import MessageIn, MessageOut
from app.schemas.common import Detail
from app.storage.rules import load_attachable, release

router = APIRouter(prefix="/messages", tags=["messages"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail}}


def _visible_or_404(db: Session, access: Access, message_id: uuid.UUID) -> Message:
    msg = lifecycle.get_or_404(db, Message, message_id, access)
    if not access.can_see_child(msg.child_id):
        raise HTTPException(404, "Message not found")
    return msg


@router.get("", response_model=list[MessageOut], responses=ERRORS)
def list_messages(child_id: uuid.UUID, include_deleted: bool = False,
                  access: Access = Depends(require_access("message:read")), db: Session = Depends(get_db)):
    """The chat of one child, oldest first."""
    if not access.can_see_child(child_id):
        raise HTTPException(404, "Child not found")
    stmt = lifecycle.apply_visibility(
        select(Message).where(Message.child_id == child_id).order_by(Message.sent_at, Message.created_at),
        Message, access, include_deleted)
    return db.scalars(stmt).all()


@router.put("/{message_id}", response_model=MessageOut, responses={201: {"model": MessageOut}, **ERRORS})
def put_message(message_id: uuid.UUID, body: MessageIn, response: Response,
                access: Access = Depends(require_access("message:write")), db: Session = Depends(get_db)):
    """Sends a message to the child's chat (PADRE of the child, or DOCENTE of their salon). Messages are never
    edited: sending the same message again (an offline retry) is a no-op, a different body for the same id is a 409."""
    if access.role not in ("PADRE", "DOCENTE"):
        raise HTTPException(403, f"{access.role} can't send messages")
    existing = db.get(Message, message_id)
    if existing is not None:
        msg = _visible_or_404(db, access, message_id)
        same = (msg.author_id == access.user_id and msg.child_id == body.child_id and msg.content == body.content
                and msg.sent_at == body.sent_at and msg.is_aviso == body.is_aviso
                and msg.attachment_file_id == body.attachment_file_id)
        if not same:
            raise HTTPException(409, "Messages can't be edited")
        return msg

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
    response.status_code = 201
    return msg


@router.delete("/{message_id}", status_code=204, responses=ERRORS)
def delete_message(message_id: uuid.UUID, access: Access = Depends(require_access("message:delete")),
                   db: Session = Depends(get_db)):
    """Only the child's parent can delete, and may delete any message of the chat (docente's too). The message
    and its photo are hidden for both sides but kept for audit."""
    if access.role != "PADRE":
        raise HTTPException(403, "Only the child's parent can delete messages")
    msg = _visible_or_404(db, access, message_id)
    lifecycle.soft_delete(db, msg)
    release(db.get(File, msg.attachment_file_id) if msg.attachment_file_id else None)
    db.commit()
    return Response(status_code=204)
