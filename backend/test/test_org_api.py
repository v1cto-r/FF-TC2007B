"""Estancias, salones and children: who may create, change, archive and delete them."""
import uuid

import pytest

from app.database.models import Child, Estancia, RecordStatus, Salon, UserRole


def put(client, path, headers, **body):
    return client.put(path, headers=headers, json=body)


# --- estancias ----------------------------------------------------------------------------

def test_org_creates_an_estancia_and_assigns_its_admin(client, db, world, auth, make_user):
    new_admin = make_user("new-admin@x", UserRole.ADMIN)
    eid = uuid.uuid4()
    r = put(client, f"/estancias/{eid}", auth(world.org), name="Nueva", address="Calle 1", admin_user_id=str(new_admin.id))
    assert r.status_code == 201 and r.json()["admin_user_id"] == str(new_admin.id) and r.json()["status"] is None
    # the new admin now sees it in their token-less scope
    assert [e["id"] for e in client.get("/estancias", headers=auth(new_admin)).json()] == [str(eid)]


def test_only_org_creates_estancias(client, world, auth):
    r = put(client, f"/estancias/{uuid.uuid4()}", auth(world.admin1), name="X", admin_user_id=str(world.admin1.id))
    assert r.status_code == 403
    assert put(client, f"/estancias/{uuid.uuid4()}", auth(world.d1), name="X").status_code == 403


def test_creating_an_estancia_needs_a_valid_free_admin(client, world, auth, make_user):
    org = auth(world.org)
    eid = lambda: f"/estancias/{uuid.uuid4()}"
    assert put(client, eid(), org, name="X").status_code == 422                                   # no admin
    assert put(client, eid(), org, name="X", admin_user_id=str(world.d1.id)).status_code == 422   # not an ADMIN
    assert put(client, eid(), org, name="X", admin_user_id=str(uuid.uuid4())).status_code == 422  # unknown
    assert put(client, eid(), org, name="X", admin_user_id=str(world.admin1.id)).status_code == 409  # already runs E1


def test_admin_edits_own_estancia_but_not_its_admin(client, world, auth, make_user):
    h = auth(world.admin1)
    r = put(client, f"/estancias/{world.e1.id}", h, name="Renamed", address="New street")
    assert r.status_code == 200 and r.json()["name"] == "Renamed" and r.json()["admin_user_id"] == str(world.admin1.id)
    other_admin = make_user("other-admin@x", UserRole.ADMIN)
    assert put(client, f"/estancias/{world.e1.id}", h, name="X", admin_user_id=str(other_admin.id)).status_code == 403
    # someone else's estancia looks like it doesn't exist
    assert put(client, f"/estancias/{world.e2.id}", h, name="Hijack").status_code == 404


def test_org_can_replace_the_admin(client, db, world, auth, make_user):
    new_admin = make_user("replacement@x", UserRole.ADMIN)
    r = put(client, f"/estancias/{world.e1.id}", auth(world.org), name="E1", admin_user_id=str(new_admin.id))
    assert r.status_code == 200 and r.json()["admin_user_id"] == str(new_admin.id)


def test_estancia_visibility_per_role(client, world, auth):
    ids = lambda u: {e["id"] for e in client.get("/estancias", headers=auth(u)).json()}
    assert ids(world.org) == {str(world.e1.id), str(world.e2.id)}
    for user in (world.admin1, world.d1, world.p1):
        assert ids(user) == {str(world.e1.id)}
    assert client.get(f"/estancias/{world.e2.id}", headers=auth(world.p1)).status_code == 404


def test_deleting_an_estancia_is_blocked_then_allowed_and_kept(client, db, world, auth):
    org = auth(world.org)
    assert client.delete(f"/estancias/{world.e1.id}", headers=org).status_code == 409   # salones, children, staff
    assert client.delete(f"/estancias/{world.e1.id}", headers=auth(world.admin1)).status_code == 403
    empty = Estancia(id=uuid.uuid4(), name="Empty", admin_user_id=world.org.id)  # not a real admin, fine for this test
    db.add(empty)
    db.commit()
    assert client.delete(f"/estancias/{empty.id}", headers=org).status_code == 204
    assert client.delete(f"/estancias/{empty.id}", headers=org).status_code == 204  # idempotent
    assert client.get(f"/estancias/{empty.id}", headers=org).json()["status"] == "DELETED"  # ORG still sees it
    assert empty.id not in {uuid.UUID(e["id"]) for e in client.get("/estancias", headers=org).json()}
    deleted = client.get("/estancias?include_deleted=true", headers=org).json()
    assert str(empty.id) in {e["id"] for e in deleted}


def test_only_org_can_ask_for_deleted_records(client, world, auth):
    for path in ("/estancias", "/salones", "/children", "/users", "/notices", "/daily-logs"):
        assert client.get(f"{path}?include_deleted=true", headers=auth(world.admin1)).status_code == 403, path


def test_archived_estancia_is_read_only_until_unarchived(client, world, auth):
    org, admin = auth(world.org), auth(world.admin1)
    assert client.post(f"/estancias/{world.e1.id}/archive", headers=org).json()["status"] == "ARCHIVED"
    assert put(client, f"/estancias/{world.e1.id}", admin, name="Nope").status_code == 409
    assert client.get(f"/estancias/{world.e1.id}", headers=admin).status_code == 200  # still listed
    assert client.post(f"/estancias/{world.e1.id}/unarchive", headers=org).json()["status"] is None
    assert put(client, f"/estancias/{world.e1.id}", admin, name="Fine").status_code == 200
    assert client.post(f"/estancias/{world.e1.id}/unarchive", headers=org).status_code == 409  # not archived


# --- salones ------------------------------------------------------------------------------

def test_admin_manages_salones_of_own_estancia(client, db, world, auth):
    h = auth(world.admin1)
    sid = uuid.uuid4()
    r = put(client, f"/salones/{sid}", h, estancia_id=str(world.e1.id), name="Nuevo", group_type="L",
            docente_id=str(world.d1.id))
    assert r.status_code == 201 and r.json()["docente_id"] == str(world.d1.id)
    r = put(client, f"/salones/{sid}", h, estancia_id=str(world.e1.id), name="Renombrado", group_type="M")
    assert r.status_code == 200 and r.json()["name"] == "Renombrado" and r.json()["docente_id"] is None
    assert put(client, f"/salones/{sid}", h, estancia_id=str(world.e2.id), name="x", group_type="M").status_code == 422


def test_salon_rules(client, world, auth):
    h = auth(world.admin1)
    body = lambda **kw: dict(estancia_id=str(world.e1.id), name=f"n{uuid.uuid4().hex[:6]}", group_type="P", **kw)
    new = lambda: f"/salones/{uuid.uuid4()}"
    assert put(client, new(), h, **body(docente_id=str(world.d3.id))).status_code == 422   # docente of another estancia
    assert put(client, new(), h, **body(docente_id=str(world.p1.id))).status_code == 422   # not a docente
    assert put(client, new(), h, estancia_id=str(world.e2.id), name="x", group_type="P").status_code == 404
    assert put(client, f"/salones/{world.s1.id}", h, estancia_id=str(world.e1.id), name="S2", group_type="P").status_code == 409  # name taken
    for who in (world.d1, world.p1, world.org):  # nobody else writes salones
        assert put(client, new(), auth(who), **body()).status_code == 403


def test_salon_visibility(client, world, auth):
    ids = lambda u: {s["id"] for s in client.get("/salones", headers=auth(u)).json()}
    assert ids(world.admin1) == {str(world.s1.id), str(world.s2.id)}
    assert ids(world.d1) == {str(world.s1.id)}
    assert ids(world.p1) == {str(world.s1.id)}
    assert ids(world.org) == {str(world.s1.id), str(world.s2.id), str(world.s3.id)}
    assert client.get(f"/salones/{world.s2.id}", headers=auth(world.d1)).status_code == 404
    assert client.get(f"/salones/{world.s3.id}", headers=auth(world.admin1)).status_code == 404


def test_salon_delete_blocked_by_active_children_only(client, db, world, auth):
    h = auth(world.admin1)
    assert client.delete(f"/salones/{world.s1.id}", headers=h).status_code == 409
    for c in (world.c1, world.c2):
        c.status = RecordStatus.ARCHIVED
    db.commit()
    assert client.delete(f"/salones/{world.s1.id}", headers=h).status_code == 204
    assert client.delete(f"/salones/{world.s1.id}", headers=auth(world.d1)).status_code == 403


# --- children -----------------------------------------------------------------------------

def test_admin_enrols_and_links_a_child(client, world, auth):
    h = auth(world.admin1)
    cid = uuid.uuid4()
    r = put(client, f"/children/{cid}", h, estancia_id=str(world.e1.id), salon_id=str(world.s2.id),
            parent_id=str(world.p2.id), full_name="Nuevo Niño", birth_date="2022-05-01")
    assert r.status_code == 201 and r.json()["parent_id"] == str(world.p2.id)
    # a parent can have several children, the new one shows up for them
    assert str(cid) in {c["id"] for c in client.get("/children", headers=auth(world.p2)).json()}
    r = put(client, f"/children/{cid}", h, estancia_id=str(world.e1.id), salon_id=str(world.s1.id),
            parent_id=str(world.p2.id), full_name="Nuevo Niño", birth_date="2022-05-01")
    assert r.status_code == 200 and r.json()["salon_id"] == str(world.s1.id)  # moved between salones


def test_child_validation(client, world, auth):
    h = auth(world.admin1)
    base = dict(estancia_id=str(world.e1.id), full_name="x", birth_date="2022-05-01")
    new = lambda: f"/children/{uuid.uuid4()}"
    assert put(client, new(), h, **base, parent_id=str(world.p3.id)).status_code == 422     # parent of another estancia
    assert put(client, new(), h, **base, parent_id=str(world.d1.id)).status_code == 422     # not a PADRE
    assert put(client, new(), h, **base, salon_id=str(world.s3.id)).status_code == 422      # salon of another estancia
    assert put(client, new(), h, estancia_id=str(world.e2.id), full_name="x", birth_date="2022-05-01").status_code == 404
    assert put(client, f"/children/{world.c1.id}", h, estancia_id=str(world.e2.id), full_name="x", birth_date="2022-05-01").status_code == 422


def test_child_birth_date_and_allergies(client, world, auth):
    h = auth(world.admin1)
    cid = uuid.uuid4()
    base = dict(estancia_id=str(world.e1.id), full_name="Alérgico")
    r = put(client, f"/children/{cid}", h, **base)    # name only: the parent fills in the rest
    assert r.status_code == 201 and r.json()["birth_date"] is None and r.json()["allergies"] == []
    assert r.json()["profile_completed_at"] is None

    r = put(client, f"/children/{cid}", h, **base, birth_date="2022-05-01", allergies=[" Peanuts ", "Lactose", "", "Peanuts"])
    assert r.json()["birth_date"] == "2022-05-01" and r.json()["allergies"] == ["Peanuts", "Lactose"]  # trimmed, deduped
    assert client.get(f"/children/{cid}", headers=auth(world.org)).json()["allergies"] == ["Peanuts", "Lactose"]

    assert put(client, f"/children/{uuid.uuid4()}", h, **base, birth_date="2999-01-01").status_code == 422
    assert put(client, f"/children/{uuid.uuid4()}", h, **base, allergies=["a,b"]).status_code == 422
    assert put(client, f"/children/{uuid.uuid4()}", h, **base, allergies=[f"{i:03}" + "x" * 97 for i in range(11)]).status_code == 422


def test_parent_completes_the_profile_and_admin_edits_keep_it(client, world, auth):
    h, cid = auth(world.admin1), uuid.uuid4()
    base = dict(estancia_id=str(world.e1.id), full_name="Pendiente", parent_id=str(world.p2.id))
    assert put(client, f"/children/{cid}", h, **base).status_code == 201
    pending = [c for c in client.get("/children", headers=auth(world.p2)).json() if c["profile_completed_at"] is None]
    assert str(cid) in {c["id"] for c in pending}                       # what the parent's app shows as pending

    url = f"/children/{cid}/profile"
    r = client.patch(url, headers=auth(world.p2), json={"birth_date": "2022-05-01", "allergies": ["Nuts", "Nuts", " Egg"]})
    assert r.status_code == 200
    assert r.json()["birth_date"] == "2022-05-01" and r.json()["allergies"] == ["Nuts", "Egg"]
    first = r.json()["profile_completed_at"]
    assert first is not None and r.json()["full_name"] == "Pendiente"
    # sending again corrects the data but keeps the first completion time
    r = client.patch(url, headers=auth(world.p2), json={"birth_date": "2022-06-01"})
    assert r.json()["allergies"] == [] and r.json()["profile_completed_at"] == first

    # an admin edit that leaves the profile fields out doesn't wipe them
    r = put(client, f"/children/{cid}", h, **{**base, "salon_id": str(world.s2.id)})
    assert r.json()["birth_date"] == "2022-06-01" and r.json()["salon_id"] == str(world.s2.id)


def test_child_profile_rules(client, world, auth):
    url = lambda c: f"/children/{c.id}/profile"
    body = {"birth_date": "2022-05-01"}
    for who in (world.d1, world.admin1, world.org):
        assert client.patch(url(world.c1), headers=auth(who), json=body).status_code == 403   # only the parent
    assert client.patch(url(world.c3), headers=auth(world.p1), json=body).status_code == 404   # another family
    assert client.patch(url(world.c1), headers=auth(world.p1), json={}).status_code == 422      # birth_date is required
    assert client.patch(url(world.c1), headers=auth(world.p1), json={"birth_date": "2999-01-01"}).status_code == 422
    # the parent can't use it to change anything else
    r = client.patch(url(world.c1), headers=auth(world.p1), json={**body, "full_name": "Hacked", "salon_id": str(world.s2.id)})
    assert r.status_code == 200 and r.json()["full_name"] == "c1" and r.json()["salon_id"] == str(world.s1.id)


def test_child_visibility_per_role(client, world, auth):
    ids = lambda u: {c["full_name"] for c in client.get("/children", headers=auth(u)).json()}
    assert ids(world.org) == {"c1", "c2", "c3", "c4"}
    assert ids(world.admin1) == {"c1", "c2", "c3"}
    assert ids(world.d1) == {"c1", "c2"}
    assert ids(world.p1) == {"c1", "c2"} and ids(world.p2) == {"c3"}
    assert client.get(f"/children/{world.c3.id}", headers=auth(world.p1)).status_code == 404   # another family
    assert client.get(f"/children/{world.c3.id}", headers=auth(world.d1)).status_code == 404   # another salon
    assert client.get(f"/children/{world.c4.id}", headers=auth(world.admin1)).status_code == 404
    assert [c["full_name"] for c in client.get(f"/children?salon_id={world.s1.id}", headers=auth(world.admin1)).json()] == ["c1", "c2"]


def test_only_admin_writes_children(client, world, auth):
    body = dict(estancia_id=str(world.e1.id), full_name="x", birth_date="2022-05-01")
    for who in (world.d1, world.p1, world.org):
        assert put(client, f"/children/{uuid.uuid4()}", auth(who), **body).status_code == 403
        assert client.delete(f"/children/{world.c1.id}", headers=auth(who)).status_code == 403


def test_deleting_a_child_soft_deletes_its_history_with_it(client, db, world, auth):
    from app.database.models import DailyLog, Message
    import datetime as dt
    log = DailyLog(id=uuid.uuid4(), child_id=world.c1.id, date=dt.date(2026, 1, 1), caregiver_name="x")
    msg = Message(id=uuid.uuid4(), child_id=world.c1.id, content="hi", sent_at=dt.datetime.now(dt.timezone.utc))
    db.add_all([log, msg])
    db.commit()
    h = auth(world.admin1)
    assert client.delete(f"/children/{world.c1.id}", headers=h).status_code == 204
    assert world.c1.id not in {uuid.UUID(c["id"]) for c in client.get("/children", headers=h).json()}
    assert client.get(f"/children/{world.c1.id}", headers=h).status_code == 404       # hidden from the admin...
    assert client.get(f"/children/{world.c1.id}", headers=auth(world.org)).json()["status"] == "DELETED"  # ...not from ORG
    db.refresh(log)
    db.refresh(msg)
    assert log.status == msg.status == RecordStatus.DELETED   # still in the database, only marked


def test_archived_child_stays_listed_but_is_read_only(client, world, auth):
    h = auth(world.admin1)
    assert client.post(f"/children/{world.c1.id}/archive", headers=h).json()["status"] == "ARCHIVED"
    assert "c1" in {c["full_name"] for c in client.get("/children", headers=auth(world.p1)).json()}
    assert put(client, f"/children/{world.c1.id}", h, estancia_id=str(world.e1.id), full_name="x").status_code == 409
    assert client.post(f"/children/{world.c1.id}/unarchive", headers=h).status_code == 200


def test_every_change_to_a_child_is_audited_with_the_actor(client, db, world, auth):
    from sqlalchemy import select
    from app.database.models import AuditLog
    h = auth(world.admin1)
    put(client, f"/children/{world.c1.id}", h, estancia_id=str(world.e1.id), salon_id=str(world.s1.id),
        parent_id=str(world.p1.id), full_name="Renamed")
    client.delete(f"/children/{world.c2.id}", headers=h)
    entries = {e.action.value: e for e in db.scalars(select(AuditLog).where(AuditLog.actor_id == world.admin1.id))}
    assert entries["UPDATE"].changes == {"full_name": ["c1", "Renamed"]}
    assert entries["DELETE"].resource_id == world.c2.id
