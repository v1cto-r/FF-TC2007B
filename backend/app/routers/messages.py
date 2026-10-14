import uuid

from pydantic import AwareDatetime
from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.auth.dependency import require_access
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import File, Message
from app.chat.hub import hub
from app.chat.service import recipients, render, visible_message
from app.schemas.comms import MessageOut
from app.schemas.common import Detail
from app.storage.rules import release

router = APIRouter(prefix="/messages", tags=["messages"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail}}


@router.get("", response_model=list[MessageOut], responses=ERRORS)
def list_messages(child_id: uuid.UUID, include_deleted: bool = False, since: AwareDatetime | None = None,
                  access: Access = Depends(require_access("message:read")), db: Session = Depends(get_db)):
    """The chat of one child, oldest first. Sending happens over the `/ws/chat` socket, not here.
    `since` (a timestamp with timezone) keeps only the messages the server stored after it: pass the `created_at` of the
    newest message you have to catch up after a reconnect."""
    if not access.can_see_child(child_id):
        raise HTTPException(404, "Child not found")
    stmt = select(Message).where(Message.child_id == child_id).order_by(Message.sent_at, Message.created_at)
    if since is not None:
        stmt = stmt.where(Message.created_at > since)
    stmt = lifecycle.apply_visibility(stmt, Message, access, include_deleted)
    return render(db, list(db.scalars(stmt)))


@router.delete("/{message_id}", status_code=204, responses=ERRORS)
def delete_message(message_id: uuid.UUID, access: Access = Depends(require_access("message:delete")),
                   db: Session = Depends(get_db)):
    """Only the child's parent can delete, and may delete any message of the chat (docente's too). The message
    and its photo are hidden for both sides but kept for audit."""
    if access.role != "PADRE":
        raise HTTPException(403, "Only the child's parent can delete messages")
    msg = visible_message(db, access, message_id)
    lifecycle.soft_delete(db, msg)
    release(db.get(File, msg.attachment_file_id) if msg.attachment_file_id else None)
    db.commit()
    people = recipients(db, msg.child_id)
    hub.publish(people, {"type": "deleted", "id": str(msg.id), "child_id": str(msg.child_id)})
    return Response(status_code=204)
