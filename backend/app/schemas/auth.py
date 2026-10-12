import uuid

from pydantic import BaseModel, Field


class TokenPair(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    pending: bool  # true until the first-time password is changed


class RefreshBody(BaseModel):
    refresh_token: str


class ChangePasswordBody(BaseModel):
    current_password: str
    new_password: str = Field(min_length=8, max_length=72)  # 72 = bcrypt's limit


class RevokeBody(BaseModel):
    user_id: uuid.UUID


class ClaimsOut(BaseModel):
    sub: str
    email: str
    full_name: str
    role: str
    estancia_id: str | None
    pending: bool
