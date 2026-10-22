"""Push notification registrations."""
import uuid
from datetime import datetime
from typing import Annotated, Literal

from pydantic import BaseModel, Field

from app.schemas.common import ORMModel


class DeviceIn(BaseModel):
    token: Annotated[str, Field(min_length=20, max_length=512)]  # FCM registration token
    platform: Literal["ANDROID"] = "ANDROID"


class DeviceOut(ORMModel):
    id: uuid.UUID
    platform: str
    last_seen: datetime
