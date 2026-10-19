"""Tags every HTTP request with a request id and writes an `http_write` line for each POST / PUT / PATCH / DELETE."""
import time
import uuid

from starlette.requests import HTTPConnection

from app.auth.token import decode_session_token
from app.observability.write_log import log, request_id
from app.rate_limit import client_ip

WRITE_METHODS = {"POST", "PUT", "PATCH", "DELETE"}


def _actor(conn: HTTPConnection) -> str | None:
    scheme, _, token = conn.headers.get("authorization", "").partition(" ")
    if scheme.lower() != "bearer":
        return None
    try:
        return decode_session_token(token).sub
    except ValueError:
        return None


class WriteLogMiddleware:
    """Plain ASGI (not BaseHTTPMiddleware) so the request id ContextVar reaches the endpoint unchanged."""

    def __init__(self, app) -> None:
        self.app = app

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        rid = uuid.uuid4().hex
        token = request_id.set(rid)
        status = 500  # unless a response starts: an exception escaped
        start = time.perf_counter()

        async def send_with_id(message) -> None:
            nonlocal status
            if message["type"] == "http.response.start":
                status = message["status"]
                message = {**message, "headers": [*message.get("headers", []), (b"x-request-id", rid.encode())]}
            await send(message)

        try:
            await self.app(scope, receive, send_with_id)
        finally:
            request_id.reset(token)
            if scope["method"] in WRITE_METHODS:
                conn = HTTPConnection(scope)
                route = scope.get("route")
                log.info({"event": "http_write", "request_id": rid, "method": scope["method"],
                          "route": getattr(route, "path", None) or scope["path"], "status": status,
                          "duration_ms": round((time.perf_counter() - start) * 1000, 1),
                          "actor_id": _actor(conn), "ip": client_ip(conn)})
