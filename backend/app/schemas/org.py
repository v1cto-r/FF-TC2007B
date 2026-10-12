"""Estancias, salones and children."""
import uuid

from pydantic import BaseModel, Field

from app.database.models import GroupType
from app.schemas.common import Name, RecordOut


class EstanciaIn(BaseModel):
    """ORG sets everything. An ADMIN may edit its own estancia but not `admin_user_id`."""
    name: Name
    address: str | None = Field(default=None, max_length=500)
    admin_user_id: uuid.UUID | None = None  # required when ORG creates
    logo_file_id: uuid.UUID | None = None  # leave out to keep the current logo; null removes it


class EstanciaOut(RecordOut):
    name: str
    address: str | None
    admin_user_id: uuid.UUID
    logo_file_id: uuid.UUID | None


class SalonIn(BaseModel):
    estancia_id: uuid.UUID
    name: Name
    group_type: GroupType
    docente_id: uuid.UUID | None = None


class SalonOut(RecordOut):
    estancia_id: uuid.UUID
    name: str
    group_type: GroupType
    docente_id: uuid.UUID | None


class ChildIn(BaseModel):
    estancia_id: uuid.UUID
    salon_id: uuid.UUID | None = None
    parent_id: uuid.UUID | None = None  # a PADRE of the same estancia
    full_name: Name


class ChildOut(RecordOut):
    estancia_id: uuid.UUID
    salon_id: uuid.UUID | None
    parent_id: uuid.UUID | None
    full_name: str
    photo_file_id: uuid.UUID | None


class ChildPhotoIn(BaseModel):
    file_id: uuid.UUID | None  # null removes the photo
