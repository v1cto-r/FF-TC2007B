from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from app.auth import service
from app.auth.access import Access
from app.auth.dependency import get_claims, require_access
from app.auth.permissions import can_manage_user
from app.auth.token import TokenClaims
from app.database.connection import get_db
from app.database.lifecycle import get_or_404
from app.database.models import User
from app.rate_limit import client_ip, hit, rate_limit
from app.schemas.auth import ChangePasswordBody, ClaimsOut, RefreshBody, RevokeBody, TokenPair
from app.schemas.common import Detail

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login", response_model=TokenPair, responses={401: {"model": Detail}, 429: {"model": Detail}},
             dependencies=[Depends(rate_limit("login-ip", 30, 60))])
def login(request: Request, form: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    # Per email+IP pair: someone hammering one account from a shared network only blocks themselves
    hit(db, f"login:{form.username.strip().lower()}:{client_ip(request)}", 5, 60)
    return service.login(db, form.username, form.password)


@router.post("/refresh", response_model=TokenPair, responses={401: {"model": Detail}},
             dependencies=[Depends(rate_limit("refresh", 30, 60))])
def refresh(body: RefreshBody, db: Session = Depends(get_db)):
    return service.refresh(db, body.refresh_token)


@router.post("/logout", status_code=204, responses={401: {"model": Detail}})
def logout(body: RefreshBody, db: Session = Depends(get_db)):
    service.logout(db, body.refresh_token)


@router.get("/me", response_model=ClaimsOut, responses={401: {"model": Detail}})
def me(claims: TokenClaims = Depends(get_claims)):
    return claims


@router.post("/change-password", response_model=TokenPair, responses={401: {"model": Detail}},
             dependencies=[Depends(rate_limit("change-password", 5, 60))])
def change_password(body: ChangePasswordBody, claims: TokenClaims = Depends(get_claims),
                    db: Session = Depends(get_db)):
    return service.change_password(db, claims.sub, body.current_password, body.new_password)


@router.post("/revoke", status_code=204, responses={403: {"model": Detail}, 404: {"model": Detail}})
def revoke(body: RevokeBody, access: Access = Depends(require_access("session:revoke")),
           db: Session = Depends(get_db)):
    """Sign a user out of every device."""
    target = get_or_404(db, User, body.user_id, access)
    if not can_manage_user(access, target):
        raise HTTPException(403, "Not allowed")
    service.revoke_user_tokens(db, target.id)
