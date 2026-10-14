import uuid

from fastapi import Depends, HTTPException
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session

from app.auth.access import Access, load_access
from app.auth.permissions import action_allowed, role_actions
from app.auth.token import TokenClaims, decode_session_token
from app.database.connection import get_db

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


def get_claims(token: str = Depends(oauth2_scheme)) -> TokenClaims:
    """Valid session token (signature + expiry) -> its claims. No DB access, no permission check."""
    try:
        return decode_session_token(token)
    except ValueError:
        raise HTTPException(401, "Invalid token", headers={"WWW-Authenticate": "Bearer"})


def _check(claims: TokenClaims, db: Session, action: str, allow_pending: bool) -> None:
    if claims.pending and not allow_pending:
        raise HTTPException(403, "Profile setup pending: change your password first")
    if not action_allowed(action, role_actions(db, claims.role)):
        raise HTTPException(403, "Not allowed")
    # Who made every change in this request: read by the audit log when the session flushes
    db.info["actor_id"] = uuid.UUID(claims.sub)


def authorize(db: Session, claims: TokenClaims, action: str) -> Access:
    """The same checks as `require_access`, for code that has no request to hang dependencies on (WebSockets)."""
    _check(claims, db, action, allow_pending=False)
    return load_access(db, claims)


def require_action(action: str, allow_pending: bool = False):
    """Dependency factory: `claims = Depends(require_action("child:read"))`."""
    def check(claims: TokenClaims = Depends(get_claims), db: Session = Depends(get_db)) -> TokenClaims:
        _check(claims, db, action, allow_pending)
        return claims
    return check


def require_access(action: str):
    """Like require_action, but also resolves what the user may reach: `access = Depends(require_access("child:read"))`."""
    def check(claims: TokenClaims = Depends(get_claims), db: Session = Depends(get_db)) -> Access:
        _check(claims, db, action, allow_pending=False)
        return load_access(db, claims)
    return check


def authenticated():
    """Any logged-in user whose profile is set up (no specific action), e.g. editing your own profile."""
    def check(claims: TokenClaims = Depends(get_claims), db: Session = Depends(get_db)) -> Access:
        if claims.pending:
            raise HTTPException(403, "Profile setup pending: change your password first")
        db.info["actor_id"] = uuid.UUID(claims.sub)
        return load_access(db, claims)
    return check
