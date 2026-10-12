"""Notices and chat messages."""
import uuid
from datetime import date, datetime

from pydantic import AwareDatetime, BaseModel

from app.schemas.common import Name, RecordOut, Text


class NoticeIn(BaseModel):
    estancia_id: uuid.UUID
    salon_id: uuid.UUID | None = None  # null = the whole estancia (ADMIN only)
    name: Name
    description: Text
    date: date


class NoticeOut(RecordOut):
    estancia_id: uuid.UUID
    salon_id: uuid.UUID | None
    author_id: uuid.UUID | None
    name: str
    description: str
    date: date


class MessageIn(BaseModel):
    child_id: uuid.UUID
    content: Text
    sent_at: AwareDatetime
    is_aviso: bool = False
    attachment_file_id: uuid.UUID | None = None


class MessageOut(RecordOut):
    child_id: uuid.UUID
    author_id: uuid.UUID | None
    content: str
    sent_at: datetime
    is_aviso: bool
    attachment_file_id: uuid.UUID | None
