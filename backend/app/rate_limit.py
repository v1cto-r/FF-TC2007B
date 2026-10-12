"""Rate limiting on Postgres, used like Redis: one row per key with a hit count and an expiry (fixed window)."""
import random
import time

from fastapi import Depends, HTTPException, Request
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.auth.token import decode_session_token
from app.database.connection import get_db

# Atomic: starts a new window when the old one expired, otherwise counts up
_HIT = text("""
INSERT INTO rate_limits (key, hits, expires_at) VALUES (:key, 1, :expires)
ON CONFLICT (key) DO UPDATE SET
  hits = CASE WHEN rate_limits.expires_at <= :now THEN 1 ELSE rate_limits.hits + 1 END,
  expires_at = CASE WHEN rate_limits.expires_at <= :now THEN :expires ELSE rate_limits.expires_at END
RETURNING hits, expires_at
""")


def client_ip(request: Request) -> str:
    # Behind Cloudflare the socket peer is Cloudflare, the real client is in this header
    return request.headers.get("cf-connecting-ip") or (request.client.host if request.client else "unknown")


def hit(db: Session, key: str, limit: int, window: int = 60) -> None:
    """Count one request for `key`; raise 429 when it goes over `limit` per `window` seconds."""
    now = time.time()
    hits, expires = db.execute(_HIT, {"key": key, "now": now, "expires": now + window}).one()
    if random.random() < 0.01: # Clean up expired, without the need of a worker
        db.execute(text("DELETE FROM rate_limits WHERE expires_at <= :now"), {"now": now})
    db.commit()
    if hits > limit:
        raise HTTPException(429, "Too many requests", headers={"Retry-After": str(max(1, int(expires - now) + 1))})


def _identity(request: Request) -> str:
    """The user id from a valid session token, otherwise the client IP."""
    auth = request.headers.get("authorization", "")
    if auth.lower().startswith("bearer "):
        try:
            return "user:" + decode_session_token(auth[7:]).sub
        except ValueError:
            pass
    return "ip:" + client_ip(request)


def rate_limit(name: str, limit: int, window: int = 60):
    """Dependency factory: `dependencies=[Depends(rate_limit("create-user", 20))]`. Per user, or per IP if not logged in."""
    def check(request: Request, db: Session = Depends(get_db)) -> None:
        hit(db, f"{name}:{_identity(request)}", limit, window)
    return check
