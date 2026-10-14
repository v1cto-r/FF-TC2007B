import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

import jwt

from app.config import settings


@dataclass
class TokenClaims:
    """What the session token says about the user. Routes get this from the auth dependencies."""
    sub: str
    email: str
    full_name: str
    role: str
    estancia_id: str | None
    pending: bool


def _encode(payload: dict, secret: str, typ: str, ttl: timedelta) -> str:
    now = datetime.now(timezone.utc)
    payload = {**payload, "typ": typ, "jti": str(uuid.uuid4()), "iat": now, "exp": now + ttl}
    return jwt.encode(payload, secret, algorithm=settings.jwt_algorithm)


def _decode(token: str, secret: str, typ: str) -> dict:
    try:
        payload = jwt.decode(token, secret, algorithms=[settings.jwt_algorithm],
                             options={"require": ["exp", "sub"]})
    except jwt.InvalidTokenError:  # includes expired
        raise ValueError("Invalid token")
    if payload.get("typ") != typ:
        raise ValueError("Wrong token type")
    return payload


def create_session_token(claims: TokenClaims) -> str:
    return _encode({**claims.__dict__}, settings.session_secret_key, "session",
                   timedelta(minutes=settings.session_ttl_minutes))


def decode_session_token(token: str) -> TokenClaims:
    p = _decode(token, settings.session_secret_key, "session")
    try:
        return TokenClaims(sub=p["sub"], email=p["email"], full_name=p["full_name"], role=p["role"],
                           estancia_id=p.get("estancia_id"), pending=bool(p.get("pending")))
    except KeyError:
        raise ValueError("Invalid token")


def session_token_expiry(token: str) -> float:
    """Unix time at which a session token stops being valid (the token must already have been validated)."""
    return float(_decode(token, settings.session_secret_key, "session")["exp"])


def create_refresh_token(user_id: str) -> str:
    return _encode({"sub": user_id}, settings.refresh_secret_key, "refresh",
                   timedelta(days=settings.refresh_ttl_days))


def decode_refresh_token(token: str) -> dict:
    return _decode(token, settings.refresh_secret_key, "refresh")
