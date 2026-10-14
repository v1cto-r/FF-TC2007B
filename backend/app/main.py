import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy.exc import IntegrityError

from app import starter
from app.auth.security import load_wordlist
from app.chat.hub import set_push
from app.config import settings
from app.database.connection import SessionLocal
from app.push.service import FcmSender, PushService
from app.rate_limit import rate_limit
from app.routers import (
    auth as auth_router, children as children_router, daily_logs as daily_logs_router,
    chat_ws as chat_ws_router, devices as devices_router, estancias as estancias_router, files as files_router, messages as messages_router,
    notices as notices_router, salones as salones_router, users as users_router,
)
from app.storage import StorageService
from app.storage.policy import load_policy
from app.storage.rules import expire_pending

log = logging.getLogger("uvicorn.error")


def _expire_pending_uploads(ttl_seconds: int) -> None:
    with SessionLocal() as db:
        n = expire_pending(db, ttl_seconds)
        if n:
            log.info("storage: expired %d unconfirmed uploads", n)


# Toda esta función corre cuando se inicia fastapi
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Creates the DB/tables if empty (and sample data when DEBUG/TEST is set). Blocking, so off the event loop.
    await asyncio.to_thread(starter.run)
    load_wordlist()  # words for first-time passwords
    app.state.storage = StorageService(settings, load_policy(settings))
    try:
        app.state.storage.ensure_buckets()
    except Exception as e:  # storage down shouldn't take the whole API down
        log.warning("Could not ensure S3 buckets: %s", e)
    await asyncio.to_thread(_expire_pending_uploads, app.state.storage.policy.upload_expires)
    set_push(None)
    if settings.firebase_credentials_path:
        try:
            set_push(PushService(lambda: app.state.session_factory(), FcmSender(settings.firebase_credentials_path)))
        except Exception as e:  # a missing or bad key shouldn't take the whole API down
            log.warning("push: notifications are off, could not load Firebase credentials: %s", e)
    else:
        log.info("push: notifications are off (FIREBASE_CREDENTIALS_PATH is not set)")
    yield

# Every endpoint gets this default limit (per user, or per IP when not logged in); routes can add stricter ones
app = FastAPI(title="ff_api", lifespan=lifespan, dependencies=[Depends(rate_limit("global", 120, 60))])
app.state.session_factory = SessionLocal  # the chat socket opens a short session per frame; tests swap this

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], #TODO: ** Cambiar a que solo la app la pueda mandar a llamar
    allow_credentials=True,
    allow_methods=["POST", "GET", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)


@app.exception_handler(IntegrityError)
async def integrity_error(request: Request, exc: IntegrityError):
    """Two requests racing for the same unique value (email, salon name, log of the day...): the loser gets a 409."""
    return JSONResponse(status_code=409, content={"detail": "Conflict: that record already exists or is in use"})


ROUTERS = (auth_router, users_router, estancias_router, salones_router, children_router, daily_logs_router,
           notices_router, messages_router, files_router, chat_ws_router, devices_router)
for r in ROUTERS:
    app.include_router(r.router)

@app.get("/health", tags=["health"])
def health():
    return {"status": "ok"}
