"""Who is connected to the chat socket right now, and how to reach them.

In memory, so it only works with a single server process (see docs/chat.md). Every connection remembers the event
loop it lives on: frames are always sent on that loop, which lets sync code (the REST routes run in a threadpool)
publish too.
"""
import asyncio
import logging
import threading
import uuid
from collections import defaultdict
from dataclasses import dataclass, field

from fastapi import WebSocket

log = logging.getLogger("uvicorn.error")


@dataclass(eq=False)
class Connection:
    user_id: uuid.UUID
    ws: WebSocket
    loop: asyncio.AbstractEventLoop
    lock: asyncio.Lock = field(default_factory=asyncio.Lock)  # one frame at a time on a socket

    async def send(self, frame: dict) -> None:
        async with self.lock:
            await self.ws.send_json(frame)


class Hub:

    def __init__(self) -> None:
        self._conns: dict[uuid.UUID, set[Connection]] = defaultdict(set)
        self._mutex = threading.Lock()

    def connect(self, user_id: uuid.UUID, ws: WebSocket) -> Connection:
        conn = Connection(user_id, ws, asyncio.get_running_loop())
        with self._mutex:
            self._conns[user_id].add(conn)
        return conn

    def disconnect(self, conn: Connection) -> None:
        with self._mutex:
            self._conns[conn.user_id].discard(conn)
            if not self._conns[conn.user_id]:
                del self._conns[conn.user_id]

    def count(self) -> int:
        """Counts open sockets, for metrics."""
        with self._mutex:
            return sum(len(conns) for conns in self._conns.values())

    def publish(self, user_ids: set[uuid.UUID], frame: dict, exclude: Connection | None = None) -> set[uuid.UUID]:
        """Sends `frame` to every open socket of `user_ids` (except `exclude`) without waiting. Callable from any
        thread. Returns the users that have no socket open: the ones a push notification would have to reach."""
        with self._mutex:
            targets = [(uid, list(self._conns.get(uid, ()))) for uid in user_ids]
        offline = {uid for uid, conns in targets if not conns}
        for _, conns in targets:
            for conn in conns:
                if conn is not exclude:
                    fut = asyncio.run_coroutine_threadsafe(conn.send(frame), conn.loop)
                    fut.add_done_callback(self._log_failure)
        return offline

    @staticmethod
    def _log_failure(fut) -> None:
        if not fut.cancelled() and fut.exception() is not None:  # a socket that died mid-send; its handler cleans up
            log.debug("chat: send failed: %r", fut.exception())


hub = Hub()


_push = None  # the PushService, when Firebase is configured (set in the app's lifespan)


def set_push(service) -> None:
    global _push
    _push = service


def notify_offline(user_ids: set[uuid.UUID], frame: dict) -> None:
    """Users who should hear about `frame` but have no socket open get a push notification (docs/push.md)."""
    if _push is not None and user_ids:
        _push.notify(user_ids, frame)
