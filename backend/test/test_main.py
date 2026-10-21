"""App-wide rules: every endpoint is typed, errors have one shape, docs stay in step."""
import pytest

from app.main import app

SPEC = app.openapi()
OPERATIONS = [(path, method, op) for path, item in SPEC["paths"].items() for method, op in item.items()
              if not path.startswith("/test/")]   # probe routes registered by other test modules
UNTYPED_OK = {("/health", "get"),          # trivial liveness probe
              ("/daily-logs/report", "get")}  # returns a PDF, not JSON


def test_health(client):
    assert client.get("/health").json() == {"status": "ok"}


@pytest.mark.parametrize("path", ["/health", "/auth/me", "/does-not-exist"])  # 200, 401 and 404 alike
def test_every_response_has_the_security_headers(client, path):
    h = client.get(path).headers
    assert h["strict-transport-security"] == "max-age=31536000; includeSubDomains"
    assert h["x-content-type-options"] == "nosniff"
    assert h["x-frame-options"] == "DENY"
    assert h["content-security-policy"] == "default-src 'none'; frame-ancestors 'none'"
    assert h["cache-control"] == "no-store"
    assert h["referrer-policy"] == "no-referrer"


def test_docs_page_gets_a_csp_that_lets_swagger_load(client):
    csp = client.get("/docs").headers["content-security-policy"]
    assert "script-src 'unsafe-inline' https://cdn.jsdelivr.net" in csp and "frame-ancestors 'none'" in csp


def test_cors_preflight_gets_the_security_headers_too(client):
    r = client.options("/health", headers={"Origin": "https://example.com", "Access-Control-Request-Method": "GET"})
    assert r.headers["x-content-type-options"] == "nosniff"


@pytest.mark.parametrize("path,method,op", OPERATIONS, ids=[f"{m.upper()} {p}" for p, m, _ in OPERATIONS])
def test_every_endpoint_has_typed_input_and_output(path, method, op):
    if (path, method) in UNTYPED_OK:
        return
    for body in [op.get("requestBody")] if op.get("requestBody") else []:
        for media in body["content"].values():
            assert "$ref" in media["schema"], f"{method.upper()} {path}: request body has no named model"
    ok = {code: r for code, r in op["responses"].items() if code.startswith("2")}
    assert ok, f"{method.upper()} {path}: no success response declared"
    for code, response in ok.items():
        if code == "204":
            continue
        schema = response.get("content", {}).get("application/json", {}).get("schema")
        assert schema is not None, f"{method.upper()} {path}: {code} has no response model"
        assert "$ref" in schema or schema.get("items", {}).get("$ref"), f"{method.upper()} {path}: {code} is not a named model"


def test_every_error_response_uses_the_detail_model():
    for path, method, op in OPERATIONS:
        for code, r in op["responses"].items():
            if code in ("401", "403", "404", "409", "429") and "content" in r:
                assert r["content"]["application/json"]["schema"]["$ref"].endswith("/Detail"), (path, method, code)


def test_operation_ids_are_unique():
    ids = [op["operationId"] for _, _, op in OPERATIONS]
    assert len(ids) == len(set(ids))


def test_no_password_field_in_any_response_model():
    for name, schema in SPEC["components"]["schemas"].items():
        if not name.endswith(("In", "Body", "Create", "Update", "Form")) and "Body_" not in name:
            assert "password_hash" not in schema.get("properties", {}), name


def test_unique_violation_race_returns_409_not_500(client, world, auth, monkeypatch):
    """Two requests passing the 'is it free?' check at once: the database constraint decides, the loser sees 409."""
    from app.routers import users
    monkeypatch.setattr(users, "_email_taken", lambda *a, **kw: False)   # pretend the check missed it
    h = auth(world.admin1)
    body = {"email": "d1@x", "full_name": "Dup", "role": "DOCENTE"}       # d1@x exists
    r = client.post("/users", headers=h, json=body)
    assert r.status_code == 409 and r.json() == {"detail": "Conflict: that record already exists or is in use"}
