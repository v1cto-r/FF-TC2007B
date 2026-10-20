"""Pickup authorizations: who besides the parent may pick a child up, and on which days."""
import uuid
from datetime import date

import pytest
from sqlalchemy import select

from app import pickups
from app.database.models import AuditAction, AuditLog, PickupAuthorization, RecordStatus

TODAY = date(2026, 10, 21)  # a Wednesday; its week starts on Monday 2026-10-19


@pytest.fixture(autouse=True)
def today(monkeypatch):
    def set_today(day):
        monkeypatch.setattr(pickups, "local_today", lambda: day)
    set_today(TODAY)
    return set_today


def body(child, kind="RECURRING", weekdays=(1, 2, 3, 4, 5), **kw):
    return {"child_id": str(child.id), "kind": kind, "weekdays": list(weekdays), "authorized_person_name": "Abuela Rosa",
            "relationship": "Abuela", "phone": "5512345678", **kw}


def put(client, headers, child, pid=None, replace=False, **kw):
    pid = pid or uuid.uuid4()
    url = f"/pickup-authorizations/{pid}" + ("?replace=true" if replace else "")
    return client.put(url, headers=headers, json=body(child, **kw))


def listed(client, headers, **params):
    r = client.get("/pickup-authorizations", headers=headers, params=params)
    assert r.status_code == 200, r.text
    return r.json()


# --- writing ------------------------------------------------------------------------------

def test_parent_creates_both_kinds(client, world, auth):
    h = auth(world.p1)
    r = put(client, h, world.c1, kind="RECURRING", weekdays=(5, 1, 3), notes="Trae INE")
    assert r.status_code == 201, r.text
    rec = r.json()
    assert rec["start_date"] == "2026-10-21" and rec["weekdays"] == [1, 3, 5] and rec["created_by"] == str(world.p1.id)
    assert rec["identification_type"] == "INE" and rec["notes"] == "Trae INE"

    r = put(client, h, world.c1, kind="THIS_WEEK", weekdays=(5,))    # a THIS_WEEK over a RECURRING: the override
    assert r.status_code == 201 and r.json()["start_date"] == "2026-10-19"


def test_this_week_cannot_add_days_that_already_passed(client, world, auth, today):
    h, pid = auth(world.p1), uuid.uuid4()
    assert put(client, h, world.c1, kind="THIS_WEEK", weekdays=(1,)).status_code == 422       # Monday is gone
    assert put(client, h, world.c1, pid, kind="THIS_WEEK", weekdays=(3,)).status_code == 201  # today is fine
    assert put(client, h, world.c1, pid, kind="THIS_WEEK", weekdays=(2, 3)).status_code == 422
    assert put(client, h, world.c1, pid, kind="THIS_WEEK", weekdays=(3, 4)).status_code == 200

    today(date(2026, 10, 22))  # Wednesday has passed, but it was already there: it may stay
    assert put(client, h, world.c1, pid, kind="THIS_WEEK", weekdays=(3, 4), phone="55").status_code == 200

    today(date(2026, 10, 26))  # next week: the old one is over and no longer listed
    assert put(client, h, world.c1, pid, kind="THIS_WEEK", weekdays=(3, 4)).status_code == 409
    assert listed(client, h) == []


def test_only_one_authorization_applies_per_day(client, world, auth):
    h = auth(world.p1)
    assert put(client, h, world.c1, kind="RECURRING", weekdays=(1, 2)).status_code == 201
    assert put(client, h, world.c1, kind="RECURRING", weekdays=(2, 3)).status_code == 409   # Tuesday is taken
    assert put(client, h, world.c1, kind="RECURRING", weekdays=(4,)).status_code == 201
    assert put(client, h, world.c1, kind="THIS_WEEK", weekdays=(5,)).status_code == 201
    assert put(client, h, world.c1, kind="THIS_WEEK", weekdays=(5, 6)).status_code == 409   # Friday is taken
    assert put(client, h, world.c1, kind="THIS_WEEK", weekdays=(6,)).status_code == 201
    assert put(client, h, world.c2, kind="RECURRING", weekdays=(1, 2)).status_code == 201   # a sibling is separate


def test_replace_cancels_what_it_overlaps(client, db, world, auth):
    h, old = auth(world.p1), uuid.uuid4()
    put(client, h, world.c1, old, kind="RECURRING", weekdays=(1, 2))
    r = put(client, h, world.c1, kind="RECURRING", weekdays=(2, 3), replace=True)
    assert r.status_code == 201
    assert db.get(PickupAuthorization, old).status == RecordStatus.DELETED
    assert client.get(f"/pickup-authorizations/{old}", headers=h).status_code == 404
    assert [p["weekdays"] for p in listed(client, h)] == [[2, 3]]


def test_on_date_gives_the_one_that_applies(client, world, auth):
    h = auth(world.p1)
    weekly = put(client, h, world.c1, kind="RECURRING", weekdays=(1, 2, 3, 4, 5)).json()["id"]
    friday = put(client, h, world.c1, kind="THIS_WEEK", weekdays=(5,)).json()["id"]
    d = auth(world.d1)
    ids = lambda day: [p["id"] for p in listed(client, d, on_date=day)]
    assert ids("2026-10-22") == [weekly]      # Thursday
    assert ids("2026-10-23") == [friday]      # this Friday the one-week authorization wins
    assert ids("2026-10-24") == []            # Saturday: nobody
    assert ids("2026-10-30") == [weekly]      # next Friday: the week is over
    assert ids("2026-10-20") == []            # before the recurring one started


def test_input_validation(client, world, auth):
    h, pid = auth(world.p1), uuid.uuid4()
    assert put(client, h, world.c1, weekdays=()).status_code == 422
    assert put(client, h, world.c1, weekdays=(8,)).status_code == 422
    assert put(client, h, world.c1, weekdays=(1, 1)).status_code == 422
    assert put(client, h, world.c1, kind="SOMETIMES").status_code == 422
    assert put(client, h, world.c1, authorized_person_name="").status_code == 422
    assert put(client, h, world.c1, pid).status_code == 201
    assert put(client, h, world.c1, pid, kind="THIS_WEEK", weekdays=(5,)).status_code == 422   # kind can't change
    assert put(client, h, world.c2, pid).status_code == 422                                   # nor the child


# --- who ----------------------------------------------------------------------------------

def test_only_the_child_s_parent_writes(client, world, auth):
    for staff in (world.d1, world.admin1, world.org):
        assert put(client, auth(staff), world.c1).status_code == 403
    assert put(client, auth(world.p2), world.c1).status_code == 404   # not their child
    assert put(client, auth(world.p1), world.c4).status_code == 404


def test_the_child_s_people_read(client, world, auth):
    pid = put(client, auth(world.p1), world.c1).json()["id"]
    for who in (world.p1, world.d1, world.admin1, world.org):
        assert [p["id"] for p in listed(client, auth(who))] == [pid]
    for who in (world.p2, world.d2, world.admin2, world.d3):
        assert listed(client, auth(who)) == []
        assert client.get(f"/pickup-authorizations/{pid}", headers=auth(who)).status_code == 404


def test_parent_cancels_and_it_is_audited(client, db, world, auth):
    h, pid = auth(world.p1), put(client, auth(world.p1), world.c1).json()["id"]
    assert client.delete(f"/pickup-authorizations/{pid}", headers=auth(world.d1)).status_code == 403
    assert client.delete(f"/pickup-authorizations/{pid}", headers=auth(world.p2)).status_code == 404
    assert client.delete(f"/pickup-authorizations/{pid}", headers=h).status_code == 204
    assert listed(client, auth(world.d1)) == []
    assert put(client, h, world.c1, pid).status_code == 404           # a cancelled one stays cancelled
    actions = db.scalars(select(AuditLog.action).where(AuditLog.resource_id == uuid.UUID(pid)).order_by(AuditLog.created_at)).all()
    assert actions == [AuditAction.CREATE, AuditAction.DELETE]
