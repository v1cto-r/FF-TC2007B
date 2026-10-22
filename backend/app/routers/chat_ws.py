"""Live chat: one WebSocket per device, `WS /ws/chat`. The protocol is documented in docs/chat.md."""
import asyncio
import json
import time
import uuid

from fastapi import APIRouter, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.concurrency import run_in_threadpool
from pydantic import ValidationError
from sqlalchemy.exc import IntegrityError

from app.auth.dependency import authorize
from app.auth.token import TokenClaims, decode_session_token, session_token_expiry
from app.chat.hub import UNAUTHORIZED, hub, notify_offline
from app.config import settings
from app.chat.members import find_chat
from app.chat.service import render, send_message
from app.rate_limit import hit
from app.schemas.comms import MessageFrame

router = APIRouter(tags=["chat"])

IDLE = 4408  # close code: no frame for too long, the client is gone or the link is dead (it reconnects)
MESSAGES_PER_MINUTE = 60


def _authenticate(factory, token: str) -> tuple[TokenClaims, float]:
    claims = decode_session_token(token)  # ValueError when invalid or expired
    with factory() as db:
        authorize(db, claims, "message:read")
    return claims, session_token_expiry(token)


def _error(frame_id, status: int, detail: str) -> dict:
    return {"type": "error", "id": frame_id, "status": status, "detail": detail}


def _handle_message(factory, claims: TokenClaims, frame: MessageFrame) -> tuple[dict, dict | None, set]:
    """Stores a message. Returns the reply for the sender, the frame for the rest of the chat and who is in it."""
    with factory() as db:
        try:
            access = authorize(db, claims, "message:write")  # fresh from the database on every message
            hit(db, f"chat:{claims.sub}", MESSAGES_PER_MINUTE, 60)
            msg, created = send_message(db, access, frame.id, frame)
            out = render(db, [msg])[0].model_dump(mode="json")
            people = find_chat(db, msg.chat_id).people if created else set()
        except HTTPException as e:
            db.rollback()
            return _error(str(frame.id), e.status_code, str(e.detail)), None, set()
        except IntegrityError:  # e.g. the photo is already on another message, or two sends of one id raced
            db.rollback()
            return _error(str(frame.id), 409, "Conflict: that record already exists or is in use"), None, set()
    return ({"type": "ack", "id": str(frame.id), "created": created, "message": out},
            {"type": "message", "message": out} if created else None, people)


@router.websocket("/ws/chat")
async def chat_socket(ws: WebSocket):
    factory = ws.app.state.session_factory
    await ws.accept()
    scheme, _, token = ws.headers.get("authorization", "").partition(" ")
    try:
        claims, expires = await run_in_threadpool(_authenticate, factory, token if scheme.lower() == "bearer" else "")
    except (ValueError, HTTPException):
        await ws.close(UNAUTHORIZED)
        return

    conn = hub.connect(uuid.UUID(claims.sub), ws)
    try:
        await conn.send({"type": "ready"})  # authenticated and registered: from here on the user receives messages
        while True:
            until_expiry = expires - time.time()
            try:
                raw = await asyncio.wait_for(ws.receive_text(), max(min(until_expiry, settings.chat_idle_seconds), 0.001))
            except asyncio.TimeoutError:
                if time.time() >= expires:  # session token expired: the client sends {"type":"auth"} before this happens
                    await ws.close(UNAUTHORIZED)
                else:  # clients ping every ~30 s; silence means a dead link, so the user must not count as online
                    await ws.close(IDLE)
                return
            try:
                data = json.loads(raw)
                kind = data.get("type")
            except (ValueError, AttributeError):
                await conn.send(_error(None, 422, "Frames are JSON objects with a type"))
                continue

            if kind == "ping":
                await conn.send({"type": "pong"})
            elif kind == "auth":  # a fresh session token keeps the socket open
                try:
                    new, new_expires = await run_in_threadpool(_authenticate, factory, str(data.get("token", "")))
                    if new.sub != claims.sub:
                        raise ValueError("Another user's token")
                except (ValueError, HTTPException):
                    await ws.close(UNAUTHORIZED)
                    return
                claims, expires = new, new_expires
                await conn.send({"type": "auth_ok"})
            elif kind == "message":
                try:
                    frame = MessageFrame.model_validate(data)
                except ValidationError as e:
                    first = e.errors()[0]
                    await conn.send(_error(data.get("id"), 422, f"{'.'.join(map(str, first['loc']))}: {first['msg']}"))
                    continue
                reply, broadcast, people = await run_in_threadpool(_handle_message, factory, claims, frame)
                await conn.send(reply)
                if broadcast:
                    offline = hub.publish(people, broadcast, exclude=conn)
                    notify_offline(offline - {conn.user_id}, broadcast)
            else:
                await conn.send(_error(data.get("id"), 422, f"Unknown frame type {kind!r}"))
    except WebSocketDisconnect:
        pass
    finally:
        hub.disconnect(conn)
