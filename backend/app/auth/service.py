import hashlib
import uuid
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException
from sqlalchemy import delete, select, update
from sqlalchemy.orm import Session

from app.auth.security import DUMMY_HASH, hash_password, verify_password
from app.auth.token import TokenClaims, create_refresh_token, create_session_token, decode_refresh_token
from app.config import settings
from app.database.models import DeviceToken, Estancia, RecordStatus, RefreshToken, User, UserProfile


def _unauthorized(detail: str = "Invalid credentials") -> HTTPException:
    return HTTPException(401, detail, headers={"WWW-Authenticate": "Bearer"})


def _hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def _estancia_id(db: Session, user: User) -> uuid.UUID | None:
    # Docentes have users.estancia_id; an ADMIN is found through estancias.admin_user_id
    return user.estancia_id or db.scalar(select(Estancia.id).where(
        Estancia.admin_user_id == user.id, Estancia.status.is_distinct_from(RecordStatus.DELETED)))


def can_sign_in(user: User | None) -> bool:
    """Not suspended (is_active) and not archived / deleted."""
    return user is not None and user.is_active and user.status is None


def issue_tokens(db: Session, user: User, family_id: uuid.UUID | None = None) -> dict:
    """New session + refresh pair. The refresh token is stored (hashed) so it can be revoked."""
    profile = db.get(UserProfile, user.id)
    pending = bool(profile and profile.pending)
    estancia_id = _estancia_id(db, user)
    claims = TokenClaims(sub=str(user.id), email=user.email, full_name=user.full_name, role=user.role.value,
                         estancia_id=str(estancia_id) if estancia_id else None, pending=pending)
    refresh = create_refresh_token(str(user.id))
    db.add(RefreshToken(
        user_id=user.id, family_id=family_id or uuid.uuid4(), token_hash=_hash_token(refresh),
        expires_at=datetime.now(timezone.utc) + timedelta(days=settings.refresh_ttl_days)))
    db.commit()
    return {"access_token": create_session_token(claims), "refresh_token": refresh,
            "token_type": "bearer", "pending": pending}


def revoke_user_tokens(db: Session, user_id: uuid.UUID) -> None:
    """Signs the user out everywhere: refresh tokens revoked and push registrations removed."""
    db.execute(delete(DeviceToken).where(DeviceToken.user_id == user_id))
    db.execute(update(RefreshToken).where(RefreshToken.user_id == user_id, RefreshToken.revoked_at.is_(None))
               .values(revoked_at=datetime.now(timezone.utc)))
    db.commit()


def login(db: Session, email: str, password: str) -> dict:
    user = db.scalar(select(User).where(
        User.email == email.strip().lower(), User.status.is_distinct_from(RecordStatus.DELETED)))
    ok = verify_password(password, user.password_hash if user else DUMMY_HASH)
    if not ok or not can_sign_in(user):
        raise _unauthorized()
    return issue_tokens(db, user)


def _find_refresh_token(db: Session, token: str) -> RefreshToken:
    try:
        decode_refresh_token(token)
    except ValueError:
        raise _unauthorized("Invalid refresh token")
    row = db.scalar(select(RefreshToken).where(RefreshToken.token_hash == _hash_token(token)))
    if row is None:
        raise _unauthorized("Invalid refresh token")
    return row


def refresh(db: Session, token: str) -> dict:
    """Rotate: the used token is revoked and a new one is issued in the same family.
    Using an already-revoked token means it leaked, so the whole family is revoked."""
    row = _find_refresh_token(db, token)
    if row.revoked_at is not None:
        db.execute(update(RefreshToken).where(RefreshToken.family_id == row.family_id, RefreshToken.revoked_at.is_(None))
                   .values(revoked_at=datetime.now(timezone.utc)))
        db.commit()
        raise _unauthorized("Refresh token already used")
    user = db.get(User, row.user_id)
    if not can_sign_in(user):
        raise _unauthorized("Invalid refresh token")
    row.revoked_at = datetime.now(timezone.utc)
    return issue_tokens(db, user, row.family_id)  # commits both changes


def logout(db: Session, token: str) -> None:
    row = _find_refresh_token(db, token)
    if row.revoked_at is None:
        row.revoked_at = datetime.now(timezone.utc)
        db.commit()


def change_password(db: Session, user_id: str, current: str, new: str) -> dict:
    """Also finishes the first-time setup (pending -> false) and signs out every other session."""
    user = db.get(User, uuid.UUID(user_id))
    if not can_sign_in(user) or not verify_password(current, user.password_hash):
        raise _unauthorized("Wrong current password")
    db.info["actor_id"] = user.id
    user.password_hash = hash_password(new)
    profile = db.get(UserProfile, user.id)
    if profile:
        profile.pending = False
    revoke_user_tokens(db, user.id)  # commits
    return issue_tokens(db, user)
