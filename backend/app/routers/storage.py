from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, Field

from app.auth.dependency import get_current_user
from app.storage import CurrentUser, InvalidUpload, RateLimitExceeded, StorageForbidden, StorageService

router = APIRouter(prefix="/storage", tags=["storage"])


class UploadRequest(BaseModel):
    bucket: str
    kind: str
    scope_id: str
    content_type: str
    size: int = Field(gt=0)


class DownloadRequest(BaseModel):
    bucket: str
    key: str


def get_storage(request: Request) -> StorageService:
    return request.app.state.storage


def _call(fn, *args):
    try:
        return fn(*args)
    except StorageForbidden as e:
        raise HTTPException(403, str(e))
    except InvalidUpload as e:
        raise HTTPException(422, str(e))
    except RateLimitExceeded as e:
        raise HTTPException(429, "Too many requests", headers={"Retry-After": str(e.retry_after)})


@router.post("/uploads")
def create_upload(body: UploadRequest, user: CurrentUser = Depends(get_current_user),
                  storage: StorageService = Depends(get_storage)):
    return _call(storage.create_upload, user, body.bucket, body.kind, body.scope_id, body.content_type, body.size)


@router.post("/downloads")
def create_download(body: DownloadRequest, user: CurrentUser = Depends(get_current_user),
                    storage: StorageService = Depends(get_storage)):
    return _call(storage.create_download, user, body.bucket, body.key)
