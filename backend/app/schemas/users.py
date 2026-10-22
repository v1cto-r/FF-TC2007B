import uuid

from pydantic import BaseModel, Field

from app.database.models import UserRole
from app.schemas.common import Name, RecordOut


class UserCreate(BaseModel):
    email: str = Field(min_length=1, max_length=255)
    full_name: Name
    role: UserRole


class UserCreated(BaseModel):
    id: uuid.UUID
    email: str
    role: UserRole
    temporary_password: str  # shown only once


class UserOut(RecordOut):
    email: str
    full_name: str
    role: UserRole
    estancia_id: uuid.UUID | None
    is_active: bool
    avatar_file_id: uuid.UUID | None


class UserUpdate(BaseModel):
    """PATCH /users/{id}: only the fields sent are changed."""
    full_name: Name | None = None
    email: str | None = Field(default=None, min_length=1, max_length=255)


class ProfileUpdate(BaseModel):
    """PATCH /users/me. `avatar_file_id: null` removes the avatar."""
    full_name: Name | None = None
    avatar_file_id: uuid.UUID | None = None


class PasswordReset(BaseModel):
    id: uuid.UUID
    temporary_password: str
