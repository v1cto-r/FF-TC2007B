import uuid
from datetime import datetime
from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field

from app.database.models import RecordStatus

Name = Annotated[str, Field(min_length=1, max_length=255)]
OptName = Annotated[str | None, Field(max_length=255)]
Text = Annotated[str, Field(max_length=10_000)]
OptText = Annotated[str | None, Field(max_length=10_000)]


class ORMModel(BaseModel):
    model_config = ConfigDict(from_attributes=True)


class RecordOut(ORMModel):
    """Columns every stored record has. `status` is null for an active record."""
    id: uuid.UUID
    status: RecordStatus | None = None
    created_at: datetime
    updated_at: datetime


class Detail(BaseModel):
    """Body of every error response."""
    detail: str
