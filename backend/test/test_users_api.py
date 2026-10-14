"""User management: create, list, edit, reset password, archive and delete."""
import uuid

from sqlalchemy import select

from app.database.models import AuditLog, RecordStatus, UserRole


def login(client, email, password="password", ip="3.3.3.3"):
    return client.post("/auth/login", data={"username": email, "password": password}, headers={"CF-Connecting-IP": ip})


def new_user(client, headers, email, role="DOCENTE", name="Nuevo"):
    return client.post("/users", headers=headers, json={"email": email, "full_name": name, "role": role})


# --- create -------------------------------------------------------------------------------

def test_admin_creates_staff_and_parents_in_their_estancia(client, world, auth):
    h = auth(world.admin1)
    for role in ("DOCENTE", "PADRE"):
        r = new_user(client, h, f"new-{role}@x", role)
        assert r.status_code == 201 and r.json()["temporary_password"].count("-") == 2
        me = client.get("/users?role=" + role, headers=h).json()
        assert any(u["email"] == f"new-{role}@x".lower() and u["estancia_id"] == str(world.e1.id) for u in me)


def test_org_creates_admins_without_an_estancia(client, world, auth):
    r = new_user(client, auth(world.org), "boss@x", "ADMIN")
    assert r.status_code == 201
    listed = [u for u in client.get("/users", headers=auth(world.org)).json() if u["email"] == "boss@x"]
    assert listed[0]["estancia_id"] is None
    # until ORG assigns an estancia, that admin can't create staff
    t = login(client, "boss@x", r.json()["temporary_password"]).json()
    changed = client.post("/auth/change-password", headers={"Authorization": f"Bearer {t['access_token']}"},
                          json={"current_password": r.json()["temporary_password"], "new_password": "a-new-password"}).json()
    h = {"Authorization": f"Bearer {changed['access_token']}"}
    assert new_user(client, h, "late@x").status_code == 400


def test_email_must_be_unique_among_live_users(client, world, auth):
    h = auth(world.admin1)
    assert new_user(client, h, "same@x").status_code == 201
    assert new_user(client, h, "SAME@x", "PADRE").status_code == 409


def test_input_is_validated(client, world, auth):
    h = auth(world.admin1)
    assert client.post("/users", headers=h, json={"email": "", "full_name": "x", "role": "DOCENTE"}).status_code == 422
    assert client.post("/users", headers=h, json={"email": "a@x", "full_name": "x" * 300, "role": "DOCENTE"}).status_code == 422
    assert client.post("/users", headers=h, json={"email": "a@x", "full_name": "x", "role": "KING"}).status_code == 422


# --- list / get ---------------------------------------------------------------------------

def test_listing_is_scoped_to_what_the_caller_manages(client, world, auth):
    emails = lambda u, q="": {x["email"] for x in client.get("/users" + q, headers=auth(u)).json()}
    assert emails(world.admin1) == {"d1@x", "d2@x", "p1@x", "p2@x"}
    assert emails(world.admin1, "?role=PADRE") == {"p1@x", "p2@x"}
    assert emails(world.org) >= {"admin1@x", "admin2@x", "d3@x", "p3@x", "org@x"}
    for who in (world.d1, world.p1):
        assert client.get("/users", headers=auth(who)).status_code == 403


def test_no_password_hash_is_ever_returned(client, world, auth):
    body = client.get("/users", headers=auth(world.org)).text + client.get("/users/me", headers=auth(world.org)).text
    assert "password" not in body and "$2b$" not in body


def test_get_user_respects_estancias(client, world, auth):
    assert client.get(f"/users/{world.d1.id}", headers=auth(world.admin1)).status_code == 200
    assert client.get(f"/users/{world.d3.id}", headers=auth(world.admin1)).status_code == 404
    assert client.get(f"/users/{world.d3.id}", headers=auth(world.org)).status_code == 200
    assert client.get(f"/users/{uuid.uuid4()}", headers=auth(world.org)).status_code == 404


# --- own profile --------------------------------------------------------------------------

def test_everyone_edits_their_own_name(client, world, auth):
    for u in (world.org, world.admin1, world.d1, world.p1):
        r = client.patch("/users/me", headers=auth(u), json={"full_name": f"Nuevo {u.email}"})
        assert r.status_code == 200 and r.json()["full_name"] == f"Nuevo {u.email}"
        assert client.get("/users/me", headers=auth(u)).json()["id"] == str(u.id)


def test_pending_users_cannot_use_the_profile_endpoints(client, db, make_user):
    u = make_user("pend@x", UserRole.DOCENTE, pending=True)
    t = login(client, "pend@x").json()
    assert client.get("/users/me", headers={"Authorization": f"Bearer {t['access_token']}"}).status_code == 403


# --- edit ---------------------------------------------------------------------------------

def test_admin_edits_name_and_email_of_their_people(client, world, auth):
    h = auth(world.admin1)
    r = client.patch(f"/users/{world.d1.id}", headers=h, json={"full_name": "Laura", "email": "LAURA@x"})
    assert r.status_code == 200 and r.json()["email"] == "laura@x" and r.json()["full_name"] == "Laura"
    assert login(client, "laura@x").status_code == 200
    assert client.patch(f"/users/{world.d2.id}", headers=h, json={"email": "laura@x"}).status_code == 409
    assert client.patch(f"/users/{world.d3.id}", headers=h, json={"full_name": "x"}).status_code == 404
    assert client.patch(f"/users/{world.d1.id}", headers=auth(world.d2), json={"full_name": "x"}).status_code == 403


# --- reset password -----------------------------------------------------------------------

def test_reset_password_forces_a_new_first_login(client, db, world, auth):
    victim_session = login(client, "p1@x").json()
    r = client.post(f"/users/{world.p1.id}/reset-password", headers=auth(world.admin1))
    assert r.status_code == 200
    temp = r.json()["temporary_password"]
    assert login(client, "p1@x", "password", ip="4.4.4.4").status_code == 401        # old password is gone
    assert client.post("/auth/refresh", json={"refresh_token": victim_session["refresh_token"]}).status_code == 401
    t = login(client, "p1@x", temp, ip="5.5.5.5").json()
    assert t["pending"] is True
    # the new hash never reaches the audit log
    entries = db.scalars(select(AuditLog).where(AuditLog.resource_id == world.p1.id,
                                                AuditLog.actor_id == world.admin1.id)).all()
    assert entries and all("$2b$" not in str(e.changes) for e in entries)


def test_reset_password_scope(client, world, auth):
    assert client.post(f"/users/{world.p3.id}/reset-password", headers=auth(world.admin1)).status_code == 404
    assert client.post(f"/users/{world.p1.id}/reset-password", headers=auth(world.d1)).status_code == 403
    assert client.post(f"/users/{world.admin1.id}/reset-password", headers=auth(world.admin1)).status_code == 404
    assert client.post(f"/users/{world.admin1.id}/reset-password", headers=auth(world.org)).status_code == 200


# --- archive / delete ---------------------------------------------------------------------

def test_archiving_blocks_login_and_refresh_until_unarchived(client, world, auth):
    session = login(client, "d2@x").json()
    r = client.post(f"/users/{world.d2.id}/archive", headers=auth(world.admin1))
    assert r.status_code == 200 and r.json()["status"] == "ARCHIVED"
    assert login(client, "d2@x", ip="6.6.6.6").status_code == 401
    assert client.post("/auth/refresh", json={"refresh_token": session["refresh_token"]}).status_code == 401
    assert "d2@x" in {u["email"] for u in client.get("/users", headers=auth(world.admin1)).json()}   # still listed
    assert client.patch(f"/users/{world.d2.id}", headers=auth(world.admin1), json={"full_name": "x"}).status_code == 409
    assert client.post(f"/users/{world.d2.id}/unarchive", headers=auth(world.admin1)).status_code == 200
    assert login(client, "d2@x", ip="7.7.7.7").status_code == 200


def test_deleting_a_user_is_soft_frees_the_email_and_blocks_login(client, db, world, auth, make_user):
    spare = make_user("spare@x", UserRole.DOCENTE, world.e1.id)   # no salon
    assert client.delete(f"/users/{spare.id}", headers=auth(world.admin1)).status_code == 204
    # deleted rows are hidden from everyone but ORG, so a repeat is a 404 for the admin and a quiet 204 for ORG
    assert client.delete(f"/users/{spare.id}", headers=auth(world.admin1)).status_code == 404
    assert client.delete(f"/users/{spare.id}", headers=auth(world.org)).status_code == 204
    assert login(client, "spare@x").status_code == 401
    assert "spare@x" not in {u["email"] for u in client.get("/users", headers=auth(world.admin1)).json()}
    assert client.get(f"/users/{spare.id}", headers=auth(world.admin1)).status_code == 404
    shown = client.get(f"/users?include_deleted=true", headers=auth(world.org)).json()
    assert {"email": "spare@x", "status": "DELETED"}.items() <= next(u for u in shown if u["email"] == "spare@x").items()
    assert new_user(client, auth(world.admin1), "spare@x").status_code == 201                    # email is free again


def test_users_with_responsibilities_cannot_be_deleted(client, world, auth):
    org, admin = auth(world.org), auth(world.admin1)
    assert client.delete(f"/users/{world.admin1.id}", headers=org).status_code == 409   # runs E1
    assert client.delete(f"/users/{world.d1.id}", headers=admin).status_code == 409     # teaches S1
    assert client.delete(f"/users/{world.p1.id}", headers=admin).status_code == 409     # has c1, c2


def test_nobody_archives_or_deletes_themselves(client, world, auth):
    for path in (f"/users/{world.org.id}/archive",):
        assert client.post(path, headers=auth(world.org)).status_code == 409
    assert client.delete(f"/users/{world.org.id}", headers=auth(world.org)).status_code == 409


def test_deletion_scope(client, world, auth):
    assert client.delete(f"/users/{world.d3.id}", headers=auth(world.admin1)).status_code == 404
    assert client.delete(f"/users/{world.d1.id}", headers=auth(world.d2)).status_code == 403


def test_an_archived_estancia_takes_no_new_people(client, db, world, auth):
    world.e1.status = RecordStatus.ARCHIVED
    db.commit()
    assert new_user(client, auth(world.admin1), "late@x").status_code == 409
