"""Estancias, salones and children."""
import uuid
from datetime import date, datetime

from pydantic import BaseModel, Field, field_validator

from app.database.models import GroupType
from app.schemas.common import Name, RecordOut

ALLERGIES_MAX_CHARS = 1000  # children.allergies is a varchar of this size


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


class _ProfileFields(BaseModel):
    """Checks shared by everything that carries `birth_date` / `allergies`."""

    @field_validator("birth_date", check_fields=False)
    @classmethod
    def _not_in_the_future(cls, v: date | None) -> date | None:
        if v is not None and v > date.today():
            raise ValueError("birth_date can't be in the future")
        return v

    @field_validator("allergies", check_fields=False)
    @classmethod
    def _clean_allergies(cls, items: list[str] | None) -> list[str] | None:
        if items is None:
            return None
        cleaned = list(dict.fromkeys(a.strip() for a in items if a.strip()))  # trimmed, no blanks, no repeats
        if any("," in a for a in cleaned):
            raise ValueError("an allergy can't contain a comma")
        if len(",".join(cleaned)) > ALLERGIES_MAX_CHARS:
            raise ValueError(f"allergies are too long (max {ALLERGIES_MAX_CHARS} characters in total)")
        return cleaned


class ChildIn(_ProfileFields):
    """The estancia's ADMIN enrols a child. `birth_date` and `allergies` are normally filled in by the parent
    (`ChildProfileIn`); leave them out here to keep what the parent sent."""
    estancia_id: uuid.UUID
    salon_id: uuid.UUID | None = None
    parent_id: uuid.UUID | None = None  # a PADRE of the same estancia
    full_name: Name
    birth_date: date | None = None
    allergies: list[str] | None = Field(default=None, max_length=50)  # stored as one comma separated string


class ChildProfileIn(_ProfileFields):
    """What the parent sends to finish the child's profile."""
    birth_date: date
    allergies: list[str] = Field(default_factory=list, max_length=50)


class ChildOut(RecordOut):
    estancia_id: uuid.UUID
    salon_id: uuid.UUID | None
    parent_id: uuid.UUID | None
    parent_name: str | None = None  # read-only, the full_name of parent_id
    full_name: str
    birth_date: date | None  # null only for children enrolled before the field existed
    allergies: list[str]
    profile_completed_at: datetime | None  # null while the parent still has to fill in the profile
    photo_file_id: uuid.UUID | None

    @field_validator("allergies", mode="before")
    @classmethod
    def _split_csv(cls, v):
        if v is None:
            return []
        return [a for a in v.split(",") if a] if isinstance(v, str) else v


class ChildPhotoIn(BaseModel):
    file_id: uuid.UUID | None  # null removes the photo

