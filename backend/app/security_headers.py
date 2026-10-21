"""Standard security headers on every HTTP response. The API only serves JSON (and the weekly report PDF), so
nothing may be loaded, framed or cached; only the interactive docs pages get a CSP that lets Swagger UI / ReDoc run."""

API_CSP = "default-src 'none'; frame-ancestors 'none'"
# FastAPI's docs pages load their bundle from jsdelivr and start it with an inline script
DOCS_CSP = ("default-src 'none'; script-src 'unsafe-inline' https://cdn.jsdelivr.net; "
            "style-src 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
            "font-src https://fonts.gstatic.com; img-src data: https://fastapi.tiangolo.com https://cdn.redoc.ly; "
            "worker-src blob:; connect-src 'self'; frame-ancestors 'none'")
DOCS_PATHS = {"/docs", "/docs/oauth2-redirect", "/redoc"}

HEADERS = [
    (b"strict-transport-security", b"max-age=31536000; includeSubDomains"),
    (b"x-content-type-options", b"nosniff"),
    (b"x-frame-options", b"DENY"),
    (b"referrer-policy", b"no-referrer"),
    (b"cross-origin-opener-policy", b"same-origin"),
    (b"cross-origin-resource-policy", b"same-origin"),
    (b"permissions-policy", b"camera=(), microphone=(), geolocation=()"),
]


class SecurityHeadersMiddleware:
    """Plain ASGI like WriteLogMiddleware. A header the endpoint already set (e.g. Cache-Control) is left alone."""

    def __init__(self, app) -> None:
        self.app = app

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        csp = DOCS_CSP if scope["path"] in DOCS_PATHS else API_CSP
        extra = [*HEADERS, (b"content-security-policy", csp.encode()), (b"cache-control", b"no-store")]

        async def send_with_headers(message) -> None:
            if message["type"] == "http.response.start":
                headers = list(message.get("headers", []))
                present = {name.lower() for name, _ in headers}
                headers += [(name, value) for name, value in extra if name not in present]
                message = {**message, "headers": headers}
            await send(message)

        await self.app(scope, receive, send_with_headers)
