"""Files (presigned upload / download) and child documents."""
import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from app.database.models import FilePurpose, UploadState
from app.schemas.common import Name, RecordOut


class FileCreate(BaseModel):
    """Asks for a signed upload URL. `id` is chosen by the client, so a retry returns the same file."""
    id: uuid.UUID
    purpose: FilePurpose
    # child id for ACTIVITY_IMAGE / MESSAGE_ATTACHMENT / CHILD_PHOTO / CHILD_DOCUMENT,
    # estancia id for ESTANCIA_LOGO, left out for USER_AVATAR (always yourself)
    target_id: uuid.UUID | None = None
    content_type: str = Field(max_length=100)
    size: int = Field(gt=0)
    original_name: str | None = Field(default=None, max_length=255)


class FileOut(RecordOut):
    purpose: FilePurpose
    kind: str
    content_type: str
    size: int
    original_name: str | None
    upload_state: UploadState
    uploaded_by: uuid.UUID
    estancia_id: uuid.UUID | None
    child_id: uuid.UUID | None


class SignedUpload(BaseModel):
    """PUT the bytes to `url` with exactly these headers, then call POST /files/{id}/complete."""
    url: str
    method: str = "PUT"
    headers: dict[str, str]
    expires_at: datetime


class UploadTicket(BaseModel):
    file: FileOut
    upload: SignedUpload


class FileDownload(BaseModel):
    file: FileOut
    url: str
    expires_at: datetime


class DocumentIn(BaseModel):
    child_id: uuid.UUID
    file_id: uuid.UUID
    title: Name


class DocumentOut(RecordOut):
    child_id: uuid.UUID
    file_id: uuid.UUID
    title: str
