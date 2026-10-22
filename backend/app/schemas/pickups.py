"""Pickup authorizations: who besides the parent may pick a child up."""
import uuid
from datetime import date
from typing import Annotated

from pydantic import BaseModel, Field, field_validator

from app.database.models import PickupKind
from app.schemas.common import Name, OptText, RecordOut

Weekday = Annotated[int, Field(ge=1, le=7)]  # ISO: 1 = Monday ... 7 = Sunday
Short = Annotated[str, Field(min_length=1, max_length=50)]


class PickupIn(BaseModel):
    """`start_date` is set by the server: this week's Monday for THIS_WEEK, today for RECURRING."""
    child_id: uuid.UUID
    kind: PickupKind
    weekdays: list[Weekday] = Field(min_length=1, max_length=7)
    authorized_person_name: Name
    relationship: Name
    phone: Short
    identification_type: Short = "INE"
    notes: OptText = None

    @field_validator("weekdays")
    @classmethod
    def _no_duplicates(cls, v: list[int]) -> list[int]:
        if len(set(v)) != len(v):
            raise ValueError("weekdays has duplicates")
        return sorted(v)


class PickupOut(RecordOut):
    child_id: uuid.UUID
    created_by: uuid.UUID | None
    kind: PickupKind
    start_date: date
    weekdays: list[int]
    authorized_person_name: str
    relationship: str
    phone: str
    identification_type: str
    notes: str | None
