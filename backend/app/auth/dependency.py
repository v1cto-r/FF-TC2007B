import uuid

from fastapi import Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session

from app.auth.token import decode_access_token
from app.database.connection import get_db
from app.database.models import User
from app.storage.scopes import load_scopes
from app.storage.service import CurrentUser

security_scheme = HTTPBearer()


def get_current_user(creds: HTTPAuthorizationCredentials = Depends(security_scheme),
                     db: Session = Depends(get_db)) -> CurrentUser:
    # The token only proves identity (sub); role and scopes come from the DB so they are never stale.
    try:
        payload = decode_access_token(creds.credentials)
        user_id = str(uuid.UUID(payload["sub"]))
    except (ValueError, KeyError):
        raise HTTPException(401, "Invalid token")
    user = db.get(User, uuid.UUID(user_id))
    if user is None or not user.is_active:
        raise HTTPException(401, "Invalid token")
    role = user.role.value
    return CurrentUser(id=user_id, role=role, scopes=load_scopes(db, user_id, role))
