"""Push notifications through Firebase Cloud Messaging (FCM). See docs/push.md.

Data-only messages that carry ids and nothing else: no names or message text go through Google. The app
wakes up, asks `GET /messages?since=` and builds the notification itself.
"""
import logging
import uuid
from concurrent.futures import ThreadPoolExecutor
from typing import Callable, ContextManager, Protocol

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.database.models import DeviceToken, User

log = logging.getLogger("uvicorn.error")


class Sender(Protocol):
    def send(self, tokens: list[str], data: dict[str, str]) -> list[str]:
        """Sends `data` to every token. Returns the tokens Firebase says are dead (to be forgotten)."""


class FcmSender:
    def __init__(self, credentials_path: str):
        import firebase_admin
        from firebase_admin import credentials
        self._app = firebase_admin.initialize_app(credentials.Certificate(credentials_path))

    def send(self, tokens: list[str], data: dict[str, str]) -> list[str]:
        from firebase_admin import messaging
        dead = []
        for i in range(0, len(tokens), 500):  # send_each takes at most 500 messages
            chunk = tokens[i:i + 500]
            messages = [messaging.Message(token=t, data=data, android=messaging.AndroidConfig(priority="high"))
                        for t in chunk]
            for token, result in zip(chunk, messaging.send_each(messages, app=self._app).responses):
                if result.success:
                    continue
                if isinstance(result.exception, (messaging.UnregisteredError, messaging.SenderIdMismatchError)):
                    dead.append(token)
                else:
                    log.warning("push: FCM refused a message: %r", result.exception)
        return dead


class PushService:

    def __init__(self, session_factory: Callable[[], ContextManager[Session]], sender: Sender, inline: bool = False):
        self._factory, self._sender = session_factory, sender
        self._pool = None if inline else ThreadPoolExecutor(max_workers=2, thread_name_prefix="push")

    def notify(self, user_ids: set[uuid.UUID], frame: dict) -> None:
        """Pushes a chat `frame` to the devices of `user_ids`. Never blocks and never raises: a push is best effort,
        the message itself is already stored."""
        if frame.get("type") != "message" or not user_ids:
            return
        if self._pool is None:
            self._send(user_ids, frame)
        else:
            self._pool.submit(self._send, user_ids, frame)

    def _send(self, user_ids: set[uuid.UUID], frame: dict) -> None:
        try:
            message = frame["message"]
            data = {"type": "message", "chat_id": str(message["chat_id"]), "message_id": str(message["id"])}
            with self._factory() as db:
                tokens = list(db.scalars(
                    select(DeviceToken.token).join(User, User.id == DeviceToken.user_id)
                    .where(DeviceToken.user_id.in_(user_ids), User.is_active, User.status.is_(None))))
                if not tokens:
                    return
                dead = self._sender.send(tokens, data)
                if dead:
                    db.execute(delete(DeviceToken).where(DeviceToken.token.in_(dead)))
                    db.commit()
        except Exception:
            log.exception("push: could not send")
