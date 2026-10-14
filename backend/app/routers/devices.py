import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import delete
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.auth.dependency import authenticated
from app.database.connection import get_db
from app.database.models import DeviceToken
from app.schemas.common import Detail
from app.schemas.devices import DeviceIn, DeviceOut

router = APIRouter(prefix="/devices", tags=["devices"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}}


@router.put("/{device_id}", response_model=DeviceOut, responses=ERRORS)
def put_device(device_id: uuid.UUID, body: DeviceIn, access: Access = Depends(authenticated()),
               db: Session = Depends(get_db)):
    """Registers this app installation for push notifications, or refreshes its FCM token (call it again whenever
    Firebase hands out a new one). The id is chosen by the app. A token that was registered by another user
    (a shared phone, a new login) moves to the caller."""
    device = db.get(DeviceToken, device_id)
    if device is not None and device.user_id != access.user_id:
        raise HTTPException(404, "Device not found")
    db.execute(delete(DeviceToken).where(DeviceToken.token == body.token, DeviceToken.id != device_id))
    if device is None:
        device = DeviceToken(id=device_id, user_id=access.user_id)
        db.add(device)
    device.token, device.platform, device.last_seen = body.token, body.platform, datetime.now(timezone.utc)
    db.commit()
    return device


@router.delete("/{device_id}", status_code=204, responses=ERRORS)
def delete_device(device_id: uuid.UUID, access: Access = Depends(authenticated()), db: Session = Depends(get_db)):
    """Stops pushes to this installation (call it on logout)."""
    device = db.get(DeviceToken, device_id)
    if device is None or device.user_id != access.user_id:
        raise HTTPException(404, "Device not found")
    db.delete(device)
    db.commit()
    return Response(status_code=204)
