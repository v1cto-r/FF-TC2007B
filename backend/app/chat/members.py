"""Who is in a chat. A chat id is a UUID that names its people:

* a child's id: the child's PADRE, the DOCENTE of their salon and the ADMIN of their estancia
* a DOCENTE's user id: that docente and the ADMIN of their estancia
* an ADMIN's user id: that admin and every ORG user

ORG reads every chat but only takes part in the ADMIN ones.
"""
import uuid
from dataclasses import dataclass

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.database.lifecycle import live
from app.database.models import Child, Estancia, RecordStatus, Salon, User, UserRole


@dataclass(frozen=True)
class Chat:
    child: Child | None  # set for a child's chat
    peer: User | None    # set for a DOCENTE / ADMIN chat
    people: frozenset[uuid.UUID]
    estancia_id: uuid.UUID | None

    @property
    def record(self) -> Child | User:
        return self.child or self.peer


def recipients(db: Session, child_id: uuid.UUID) -> set[uuid.UUID]:
    """The people of a child's chat: their parent, the docente of their salon and the admin of their estancia."""
    row = db.execute(
        select(Child.parent_id, Salon.docente_id, Estancia.admin_user_id)
        .join(Salon, Salon.id == Child.salon_id, isouter=True)
        .join(Estancia, Estancia.id == Child.estancia_id, isouter=True)
        .where(Child.id == child_id)).first()
    return {u for u in row or () if u is not None}


def find_chat(db: Session, chat_id: uuid.UUID) -> Chat | None:
    """The chat `chat_id` names, whoever asks. None when it names nothing that has a chat."""
    child = db.get(Child, chat_id)
    if child is not None:
        return Chat(child, None, frozenset(recipients(db, child.id)), child.estancia_id)
    peer = db.get(User, chat_id)
    if peer is None:
        return None
    if peer.role == UserRole.DOCENTE:
        admin = db.scalar(select(Estancia.admin_user_id).where(Estancia.id == peer.estancia_id, live(Estancia)))
        return Chat(None, peer, frozenset(u for u in (peer.id, admin) if u is not None), peer.estancia_id)
    if peer.role == UserRole.ADMIN:
        orgs = db.scalars(select(User.id).where(User.role == UserRole.ORG, live(User)))
        estancia = db.scalar(select(Estancia.id).where(Estancia.admin_user_id == peer.id, live(Estancia)))
        return Chat(None, peer, frozenset({peer.id, *orgs}), estancia)
    return None


def open_chat(db: Session, access: Access, chat_id: uuid.UUID) -> Chat:
    """A chat the caller may read: ORG reads them all (deleted ones too), everyone else only the chats they are in."""
    chat = find_chat(db, chat_id)
    if chat is None:
        raise HTTPException(404, "Chat not found")
    if chat.child is not None:
        allowed = access.can_see_child(chat.child.id)
    else:
        allowed = access.is_org or (chat.peer.status != RecordStatus.DELETED and access.user_id in chat.people)
    if not allowed:
        raise HTTPException(404, "Chat not found")
    return chat
