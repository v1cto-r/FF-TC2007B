import uuid

import pytest
from fastapi import Depends

from app.auth import token as tokens
from app.auth.dependency import require_action
from app.auth.permissions import action_allowed
from app.database.models import Estancia, User, UserRole
from app.main import app


# A route protected by an action, to test the dependency
@app.get("/test/log-write")
def _child(claims=Depends(require_action("daily_log:write"))):
    return {"ok": True}


def login(client, email="a@x", password="password", ip="1.1.1.1"):
    return client.post("/auth/login", data={"username": email, "password": password},
                       headers={"CF-Connecting-IP": ip})


def bearer(tokens_json):
    return {"Authorization": f"Bearer {tokens_json['access_token']}"}


# --- tokens -------------------------------------------------------------------------------

def test_keys_are_not_interchangeable():
    refresh = tokens.create_refresh_token("u1")
    with pytest.raises(ValueError):
        tokens.decode_session_token(refresh)
    claims = tokens.TokenClaims("u1", "a@x", "A", "ADMIN", None, False)
    with pytest.raises(ValueError):
        tokens.decode_refresh_token(tokens.create_session_token(claims))


def test_expired_and_tampered_tokens_rejected(monkeypatch):
    claims = tokens.TokenClaims("u1", "a@x", "A", "ADMIN", None, False)
    monkeypatch.setattr(tokens.settings, "session_ttl_minutes", -1)
    with pytest.raises(ValueError):
        tokens.decode_session_token(tokens.create_session_token(claims))
    monkeypatch.setattr(tokens.settings, "session_ttl_minutes", 5)
    good = tokens.create_session_token(claims)
    with pytest.raises(ValueError):
        tokens.decode_session_token(good[:-3] + "abc")


def test_action_allowed():
    assert action_allowed("child:read", {"child:read"})
    assert action_allowed("child:write", {"child:*"})
    assert action_allowed("notice:read", {"*"})
    assert not action_allowed("child:write", {"child:read", "notice:*"})


# --- login / me ---------------------------------------------------------------------------

def test_login_and_me(client, db, make_user):
    make_user("a@x", UserRole.DOCENTE)
    r = login(client)
    assert r.status_code == 200 and r.json()["pending"] is False
    me = client.get("/auth/me", headers=bearer(r.json())).json()
    assert me["email"] == "a@x" and me["role"] == "DOCENTE"


def test_login_failures(client, db, make_user):
    make_user("a@x", UserRole.DOCENTE)
    make_user("off@x", UserRole.DOCENTE, active=False)
    assert login(client, password="wrong").status_code == 401
    assert login(client, email="nobody@x").status_code == 401
    assert login(client, email="off@x").status_code == 401
    assert client.get("/auth/me").status_code == 401
    assert client.get("/auth/me", headers={"Authorization": "Bearer junk"}).status_code == 401


def test_admin_token_has_estancia(client, db, make_user):
    admin = make_user("a@x", UserRole.ADMIN)
    est = Estancia(id=uuid.uuid4(), name="E", admin_user_id=admin.id)
    db.add(est)
    db.commit()
    me = client.get("/auth/me", headers=bearer(login(client).json())).json()
    assert me["estancia_id"] == str(est.id)


# --- refresh ------------------------------------------------------------------------------

def test_refresh_rotates_and_detects_reuse(client, db, make_user):
    make_user("a@x", UserRole.DOCENTE)
    first = login(client).json()
    second = client.post("/auth/refresh", json={"refresh_token": first["refresh_token"]})
    assert second.status_code == 200
    second = second.json()
    assert second["refresh_token"] != first["refresh_token"]

    # replaying the old token fails and burns the whole family, including the new token
    assert client.post("/auth/refresh", json={"refresh_token": first["refresh_token"]}).status_code == 401
    assert client.post("/auth/refresh", json={"refresh_token": second["refresh_token"]}).status_code == 401


def test_refresh_rejects_session_token_and_garbage(client, db, make_user):
    make_user("a@x", UserRole.DOCENTE)
    t = login(client).json()
    assert client.post("/auth/refresh", json={"refresh_token": t["access_token"]}).status_code == 401
    assert client.post("/auth/refresh", json={"refresh_token": "nope"}).status_code == 401


def test_logout_revokes_refresh_token(client, db, make_user):
    make_user("a@x", UserRole.DOCENTE)
    t = login(client).json()
    assert client.post("/auth/logout", json={"refresh_token": t["refresh_token"]}).status_code == 204
    assert client.post("/auth/refresh", json={"refresh_token": t["refresh_token"]}).status_code == 401


# --- actions ------------------------------------------------------------------------------

def test_require_action(client, db, make_user):
    make_user("doc@x", UserRole.DOCENTE)
    make_user("org@x", UserRole.ORG)
    assert client.get("/test/log-write").status_code == 401
    assert client.get("/test/log-write", headers=bearer(login(client, "doc@x").json())).status_code == 200
    assert client.get("/test/log-write", headers=bearer(login(client, "org@x").json())).status_code == 403


def test_pending_user_only_reaches_setup_endpoints(client, db, make_user):
    make_user("a@x", UserRole.DOCENTE, pending=True)
    t = login(client).json()
    assert t["pending"] is True
    assert client.get("/test/log-write", headers=bearer(t)).status_code == 403
    assert client.get("/auth/me", headers=bearer(t)).status_code == 200

    r = client.post("/auth/change-password", headers=bearer(t),
                    json={"current_password": "password", "new_password": "a-new-password"})
    assert r.status_code == 200 and r.json()["pending"] is False
    assert client.get("/test/log-write", headers=bearer(r.json())).status_code == 200
    # old refresh token and old password no longer work
    assert client.post("/auth/refresh", json={"refresh_token": t["refresh_token"]}).status_code == 401
    assert login(client, password="password").status_code == 401
    assert login(client, password="a-new-password").status_code == 200


def test_change_password_needs_current_password(client, db, make_user):
    make_user("a@x", UserRole.DOCENTE)
    t = login(client).json()
    r = client.post("/auth/change-password", headers=bearer(t),
                    json={"current_password": "wrong", "new_password": "a-new-password"})
    assert r.status_code == 401


# --- creating users / revoking ------------------------------------------------------------

def test_admin_creates_docente_who_must_change_password(client, db, make_user):
    admin = make_user("admin@x", UserRole.ADMIN)
    est = Estancia(id=uuid.uuid4(), name="E", admin_user_id=admin.id)
    db.add(est)
    db.commit()
    h = bearer(login(client, "admin@x").json())

    r = client.post("/users", headers=h, json={"email": "New@X", "full_name": "N", "role": "DOCENTE"})
    assert r.status_code == 201
    temp = r.json()["temporary_password"]

    t = login(client, "new@x", temp).json()
    assert t["pending"] is True
    me = client.get("/auth/me", headers=bearer(t)).json()
    assert me["estancia_id"] == str(est.id)  # docente joined the admin's estancia

    assert client.post("/users", headers=h, json={"email": "new@x", "full_name": "N", "role": "PADRE"}).status_code == 409


def test_create_user_role_rules(client, db, make_user):
    make_user("admin@x", UserRole.ADMIN)
    make_user("org@x", UserRole.ORG)
    make_user("doc@x", UserRole.DOCENTE)
    body = lambda role: {"email": f"{role}@n", "full_name": "N", "role": role}
    admin, org, doc = (bearer(login(client, e).json()) for e in ("admin@x", "org@x", "doc@x"))

    assert client.post("/users", headers=admin, json=body("ADMIN")).status_code == 403
    assert client.post("/users", headers=admin, json=body("ORG")).status_code == 403
    assert client.post("/users", headers=org, json=body("DOCENTE")).status_code == 403
    assert client.post("/users", headers=org, json=body("ADMIN")).status_code == 201
    assert client.post("/users", headers=doc, json=body("PADRE")).status_code == 403  # no user:create


def test_revoke_user_sessions(client, db, make_user):
    admin = make_user("admin@x", UserRole.ADMIN)
    other_admin = make_user("admin2@x", UserRole.ADMIN)
    est = Estancia(id=uuid.uuid4(), name="E", admin_user_id=admin.id)
    other_est = Estancia(id=uuid.uuid4(), name="E2", admin_user_id=other_admin.id)
    db.add_all([est, other_est])
    db.commit()
    mine = make_user("mine@x", UserRole.DOCENTE, estancia_id=est.id)
    other = make_user("other@x", UserRole.DOCENTE, estancia_id=other_est.id)
    h = bearer(login(client, "admin@x").json())
    victim = login(client, "mine@x").json()

    # a user of another estancia looks like it doesn't exist to this admin
    assert client.post("/auth/revoke", headers=h, json={"user_id": str(other.id)}).status_code == 403
    assert client.post("/auth/revoke", headers=h, json={"user_id": str(uuid.uuid4())}).status_code == 404
    assert client.post("/auth/revoke", headers=h, json={"user_id": str(mine.id)}).status_code == 204
    assert client.post("/auth/refresh", json={"refresh_token": victim["refresh_token"]}).status_code == 401
    # a docente has no session:revoke
    assert client.post("/auth/revoke", headers=bearer(login(client, "other@x").json()),
                       json={"user_id": str(mine.id)}).status_code == 403


# --- rate limiting ------------------------------------------------------------------------

def test_login_rate_limit_is_per_email_and_ip(client, db, make_user):
    make_user("a@x", UserRole.DOCENTE)
    make_user("b@x", UserRole.DOCENTE)
    for _ in range(5):
        assert login(client, "a@x", "wrong").status_code == 401
    blocked = login(client, "a@x", "wrong")
    assert blocked.status_code == 429 and int(blocked.headers["Retry-After"]) >= 1
    assert login(client, "a@x").status_code == 429                      # even the right password, same pair
    assert login(client, "b@x").status_code == 200                      # same IP, other account
    assert login(client, "a@x", ip="2.2.2.2").status_code == 200        # same account, other IP


def test_authenticated_limit_is_per_user_not_ip(client, db, make_user):
    from app.rate_limit import hit
    make_user("a@x", UserRole.DOCENTE)
    make_user("b@x", UserRole.DOCENTE)
    a, b = (bearer(login(client, e, ip=f"9.9.9.{i}").json()) for i, e in enumerate(("a@x", "b@x")))
    # exhaust user a's global bucket directly, then see that b (same machine) is unaffected
    sub_a = client.get("/auth/me", headers=a).json()["sub"]
    for _ in range(119):  # the call above already counted one
        hit(db, f"global:user:{sub_a}", 120, 60)
    assert client.get("/auth/me", headers=a).status_code == 429
    assert client.get("/auth/me", headers=b).status_code == 200


# --- default ORG user ---------------------------------------------------------------------

def test_default_org_user_is_created_once_and_pending(client, db):
    from app import starter
    from app.config import settings
    engine = db.get_bind()
    starter._ensure_default_org(engine, settings)
    starter._ensure_default_org(engine, settings)  # second run does nothing
    assert db.query(User).filter(User.role == UserRole.ORG).count() == 1

    t = login(client, settings.default_org_email, settings.default_org_password).json()
    assert t["pending"] is True
    r = client.post("/auth/change-password", headers=bearer(t),
                    json={"current_password": settings.default_org_password, "new_password": "a-new-password"})
    assert r.json()["pending"] is False
    org = bearer(r.json())
    assert client.post("/users", headers=org, json={"email": "e@x", "full_name": "E", "role": "ADMIN"}).status_code == 201


def test_default_org_skipped_when_an_org_exists(db, make_user):
    from app import starter
    from app.config import settings
    make_user("org@x", UserRole.ORG)
    starter._ensure_default_org(db.get_bind(), settings)
    assert db.query(User).count() == 1


# --- first-time password --------------------------------------------------------------------

def test_placeholder_password_is_three_words():
    from app.auth.security import WORDS, placeholder_password
    parts = placeholder_password().split("-")
    assert len(parts) == 3 and all(p in WORDS for p in parts)


def test_placeholder_password_needs_loaded_wordlist():
    from app.auth import security
    security.WORDS.clear()
    with pytest.raises(RuntimeError):
        security.placeholder_password()
