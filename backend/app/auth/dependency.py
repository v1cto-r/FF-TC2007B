from fastapi import Depends, HTTPException
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session

from app.auth.permissions import action_allowed, role_actions
from app.auth.token import TokenClaims, decode_session_token
from app.database.connection import get_db
from app.storage.scopes import load_scopes
from app.storage.service import CurrentUser

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


def get_claims(token: str = Depends(oauth2_scheme)) -> TokenClaims:
    """Valid session token (signature + expiry) -> its claims. No DB access, no permission check."""
    try:
        return decode_session_token(token)
    except ValueError:
        raise HTTPException(401, "Invalid token", headers={"WWW-Authenticate": "Bearer"})


def require_action(action: str, allow_pending: bool = False):
    """Dependency factory: `claims = Depends(require_action("child:read"))`."""
    def check(claims: TokenClaims = Depends(get_claims), db: Session = Depends(get_db)) -> TokenClaims:
        if claims.pending and not allow_pending:
            raise HTTPException(403, "Profile setup pending: change your password first")
        if not action_allowed(action, role_actions(db, claims.role)):
            raise HTTPException(403, "Not allowed")
        return claims
    return check


def get_current_user(claims: TokenClaims = Depends(get_claims), db: Session = Depends(get_db)) -> CurrentUser:
    """Used by the storage router, which does its own policy checks from the user's scopes."""
    if claims.pending:
        raise HTTPException(403, "Profile setup pending: change your password first")
    return CurrentUser(id=claims.sub, role=claims.role, scopes=load_scopes(db, claims.sub, claims.role))
