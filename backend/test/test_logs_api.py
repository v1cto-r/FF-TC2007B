"""Daily logs (check-in / stay / check-out) and activities with photos."""
import uuid
from datetime import date

import pytest
from sqlalchemy import select

from app.database.models import (
    ActivityImage, ActivityRecord, CheckIn, DailyLog, File, FilePurpose, RecordStatus, Stay, UploadState,
)

CHECK_IN = dict(check_in_time="08:05:00", dropped_off_by_name="Mamá", received_by_staff_name="Seño", is_sick=False,
                arrives_with_symptoms=False, arrived_clean=True, brought_complete_backpack=True,
                good_physical_condition=True)
STAY = dict(feeding_breakfast="ALL", feeding_snack="SOME", feeding_lunch="ASKED_FOR_MORE", slept=True, nap_minutes=45,
            mood="Feliz", cried=False, fought=False, participated=True, had_accident=False, health_issue=False)
CHECK_OUT = dict(check_out_time="16:30:00", released_in_good_physical_condition=True, released_clean=True,
                 belongings_complete=True, picked_up_by_name="Papá", released_by_staff_name="Seño")


def log_body(child, day="2026-03-02", **parts):
    return dict(child_id=str(child.id), date=day, caregiver_name="Seño Laura", **parts)


def put_log(client, headers, child, log_id=None, **kw):
    log_id = log_id or uuid.uuid4()
    return client.put(f"/daily-logs/{log_id}", headers=headers, json=log_body(child, **kw))


# --- writing ------------------------------------------------------------------------------

def test_docente_records_a_whole_day(client, world, auth):
    r = put_log(client, auth(world.d1), world.c1, check_in=CHECK_IN, stay=STAY, check_out=CHECK_OUT)
    assert r.status_code == 201
    body = r.json()
    assert body["caregiver_id"] == str(world.d1.id) and body["caregiver_name"] == "Seño Laura"
    assert body["check_in"]["dropped_off_by_name"] == "Mamá" and body["check_in"]["status"] is None
    assert body["stay"]["feeding_lunch"] == "ASKED_FOR_MORE" and body["stay"]["nap_minutes"] == 45
    assert body["check_out"]["picked_up_by_name"] == "Papá" and body["activities"] == []


def test_stages_arrive_one_at_a_time_and_are_replaced_in_place(client, db, world, auth):
    h, lid = auth(world.d1), uuid.uuid4()
    assert put_log(client, h, world.c1, lid, check_in=CHECK_IN).status_code == 201
    r = put_log(client, h, world.c1, lid, stay=STAY)           # check_in left out: untouched
    assert r.status_code == 200 and r.json()["check_in"] and r.json()["stay"] and r.json()["check_out"] is None
    r = put_log(client, h, world.c1, lid, check_in={**CHECK_IN, "is_sick": True, "symptoms_description": "tos"})
    assert r.json()["check_in"]["is_sick"] is True and r.json()["stay"]["mood"] == "Feliz"
    assert db.scalars(select(CheckIn).where(CheckIn.daily_log_id == lid)).all().__len__() == 1  # updated, not duplicated


def test_who_can_write_logs(client, world, auth):
    body = lambda c: log_body(c, check_in=CHECK_IN)
    new = lambda: f"/daily-logs/{uuid.uuid4()}"
    assert client.put(new(), headers=auth(world.d1), json=body(world.c3)).status_code == 404   # other salon
    assert client.put(new(), headers=auth(world.d1), json=body(world.c4)).status_code == 404   # other estancia
    assert client.put(new(), headers=auth(world.d2), json=body(world.c3)).status_code == 201
    assert client.put(new(), headers=auth(world.p1), json=body(world.c1)).status_code == 403   # parents only read
    assert client.put(new(), headers=auth(world.org), json=body(world.c1)).status_code == 403  # ORG only reads
    assert client.put(new(), headers=auth(world.admin2), json=body(world.c1)).status_code == 404


def test_admin_can_correct_a_log_but_not_delete_it(client, world, auth):
    lid = uuid.uuid4()
    put_log(client, auth(world.d1), world.c1, lid, stay=STAY)
    r = put_log(client, auth(world.admin1), world.c1, lid, stay={**STAY, "mood": "Triste"})
    assert r.status_code == 200 and r.json()["stay"]["mood"] == "Triste"
    assert r.json()["caregiver_id"] == str(world.d1.id)       # the original caregiver is kept
    assert client.delete(f"/daily-logs/{lid}", headers=auth(world.admin1)).status_code == 403


def test_input_validation(client, world, auth):
    h = auth(world.d1)
    assert put_log(client, h, world.c1, stay={**STAY, "pee_count": -1}).status_code == 422
    assert put_log(client, h, world.c1, stay={**STAY, "mood": "x" * 300}).status_code == 422
    assert put_log(client, h, world.c1, stay={**STAY, "feeding_lunch": "LOTS"}).status_code == 422
    assert put_log(client, h, world.c1, check_in={**CHECK_IN, "check_in_time": "late"}).status_code == 422
    assert put_log(client, h, world.c1, check_in={k: v for k, v in CHECK_IN.items() if k != "is_sick"}).status_code == 422
    assert put_log(client, h, world.c1, day="not-a-date").status_code == 422


def test_one_log_per_child_per_day(client, world, auth):
    h = auth(world.d1)
    assert put_log(client, h, world.c1, day="2026-04-01").status_code == 201
    assert put_log(client, h, world.c1, day="2026-04-01").status_code == 409
    assert put_log(client, h, world.c2, day="2026-04-01").status_code == 201   # sibling, same day


def test_a_log_cannot_move_to_another_child(client, world, auth):
    h, lid = auth(world.d1), uuid.uuid4()
    put_log(client, h, world.c1, lid)
    assert put_log(client, h, world.c2, lid).status_code == 422


def test_archived_child_is_read_only(client, db, world, auth):
    lid = uuid.uuid4()
    put_log(client, auth(world.d1), world.c1, lid, check_in=CHECK_IN)
    world.c1.status = RecordStatus.ARCHIVED
    db.commit()
    assert put_log(client, auth(world.d1), world.c1, lid, check_in=CHECK_IN).status_code == 409
    assert put_log(client, auth(world.d1), world.c1, day="2026-09-09").status_code == 409
    assert client.get(f"/daily-logs/{lid}", headers=auth(world.p1)).status_code == 200   # parents still read


# --- reading ------------------------------------------------------------------------------

@pytest.fixture
def two_logs(client, world, auth):
    a, b = uuid.uuid4(), uuid.uuid4()
    put_log(client, auth(world.d1), world.c1, a, day="2026-05-01", check_in=CHECK_IN)
    put_log(client, auth(world.d2), world.c3, b, day="2026-05-01", stay=STAY)
    return a, b


def test_logs_are_visible_only_to_the_child_s_people(client, world, auth, two_logs):
    a, b = two_logs
    ids = lambda u, q="": {x["id"] for x in client.get("/daily-logs" + q, headers=auth(u)).json()}
    assert ids(world.p1) == {str(a)} and ids(world.p2) == {str(b)}
    assert ids(world.d1) == {str(a)} and ids(world.d2) == {str(b)}
    assert ids(world.admin1) == {str(a), str(b)} and ids(world.admin2) == set()
    assert ids(world.org) == {str(a), str(b)}
    assert client.get(f"/daily-logs/{b}", headers=auth(world.p1)).status_code == 404
    assert client.get(f"/daily-logs/{b}", headers=auth(world.d1)).status_code == 404
    assert client.get(f"/daily-logs/{uuid.uuid4()}", headers=auth(world.org)).status_code == 404


def test_log_filters(client, world, auth):
    h = auth(world.admin1)
    for day in ("2026-06-01", "2026-06-15", "2026-07-01"):
        put_log(client, auth(world.d1), world.c1, day=day)
    days = lambda q: [x["date"] for x in client.get("/daily-logs" + q, headers=h).json()]
    assert days(f"?child_id={world.c1.id}") == ["2026-07-01", "2026-06-15", "2026-06-01"]   # newest first
    assert days("?from_date=2026-06-10&to_date=2026-06-30") == ["2026-06-15"]
    assert days(f"?child_id={world.c2.id}") == []


# --- deleting -----------------------------------------------------------------------------

def test_deleting_a_log_deletes_its_parts_but_keeps_them(client, db, world, auth):
    h, lid = auth(world.d1), uuid.uuid4()
    put_log(client, h, world.c1, lid, check_in=CHECK_IN, stay=STAY, check_out=CHECK_OUT)
    aid = uuid.uuid4()
    client.put(f"/activities/{aid}", headers=h, json=dict(daily_log_id=str(lid), name="Pintura"))

    assert client.delete(f"/daily-logs/{lid}", headers=h).status_code == 204
    assert client.get(f"/daily-logs/{lid}", headers=h).status_code == 404
    assert client.get(f"/daily-logs/{lid}", headers=auth(world.p1)).status_code == 404
    assert db.get(CheckIn, db.scalar(select(CheckIn.id).where(CheckIn.daily_log_id == lid))).status == RecordStatus.DELETED
    assert db.scalar(select(Stay.status).where(Stay.daily_log_id == lid)) == RecordStatus.DELETED
    assert db.get(ActivityRecord, aid).status == RecordStatus.DELETED

    # ORG can still see all of it
    full = client.get(f"/daily-logs/{lid}", headers=auth(world.org)).json()
    assert full["status"] == "DELETED" and full["check_in"]["status"] == "DELETED" and full["activities"][0]["status"] == "DELETED"
    assert put_log(client, h, world.c1, day="2026-03-02").status_code == 201   # the day can be recorded again


def test_only_the_docente_deletes_logs(client, world, auth):
    lid = uuid.uuid4()
    put_log(client, auth(world.d1), world.c1, lid)
    assert client.delete(f"/daily-logs/{lid}", headers=auth(world.d2)).status_code == 404
    assert client.delete(f"/daily-logs/{lid}", headers=auth(world.p1)).status_code == 403
    assert client.delete(f"/daily-logs/{lid}", headers=auth(world.org)).status_code == 403


# --- activities ---------------------------------------------------------------------------

@pytest.fixture
def a_log(client, world, auth):
    lid = uuid.uuid4()
    put_log(client, auth(world.d1), world.c1, lid)
    return lid


def photo(make_file, world, who=None, child=None, **kw):
    return make_file(who or world.d1, FilePurpose.ACTIVITY_IMAGE, child=child or world.c1, **kw)


def put_activity(client, headers, log_id, images=(), aid=None, name="Pintura"):
    aid = aid or uuid.uuid4()
    return client.put(f"/activities/{aid}", headers=headers,
                      json=dict(daily_log_id=str(log_id), name=name, image_file_ids=[str(i) for i in images]))


def test_activity_with_ordered_photos(client, db, world, auth, a_log, make_file):
    h = auth(world.d1)
    f1, f2, f3 = (photo(make_file, world) for _ in range(3))
    aid = uuid.uuid4()
    r = put_activity(client, h, a_log, [f1.id, f2.id], aid)
    assert r.status_code == 201 and [i["file_id"] for i in r.json()["images"]] == [str(f1.id), str(f2.id)]
    assert [i["position"] for i in r.json()["images"]] == [0, 1]

    r = put_activity(client, h, a_log, [f2.id, f3.id, f1.id], aid, name="Pintura 2")   # reorder + add
    assert r.status_code == 200 and r.json()["name"] == "Pintura 2"
    assert [i["file_id"] for i in r.json()["images"]] == [str(f2.id), str(f3.id), str(f1.id)]

    r = put_activity(client, h, a_log, [f3.id], aid)                                    # remove two
    assert [i["file_id"] for i in r.json()["images"]] == [str(f3.id)]
    db.expire_all()
    assert db.get(File, f1.id).status == RecordStatus.DELETED and db.get(File, f3.id).status is None   # file released, kept
    gone = db.scalars(select(ActivityImage).where(ActivityImage.file_id == f1.id)).all()
    assert gone and all(i.status == RecordStatus.DELETED for i in gone)

    log = client.get(f"/daily-logs/{a_log}", headers=auth(world.p1)).json()               # the parent sees the result
    assert [i["file_id"] for i in log["activities"][0]["images"]] == [str(f3.id)]


def test_photos_must_be_ready_and_belong_to_this_child_and_purpose(client, world, auth, a_log, make_file):
    h = auth(world.d1)
    bad = {
        "pending": photo(make_file, world, state=UploadState.PENDING),
        "other child": photo(make_file, world, child=world.c2),
        "wrong purpose": make_file(world.d1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1),
        "someone else's": photo(make_file, world, who=world.d2),
        "deleted": photo(make_file, world, status=RecordStatus.DELETED),
    }
    for why, f in bad.items():
        assert put_activity(client, h, a_log, [f.id]).status_code == 422, why
    assert put_activity(client, h, a_log, [uuid.uuid4()]).status_code == 422
    ok = photo(make_file, world)
    assert put_activity(client, h, a_log, [ok.id, ok.id]).status_code == 422          # duplicates


def test_a_photo_belongs_to_one_activity(client, world, auth, a_log, make_file):
    h, f = auth(world.d1), photo(make_file, world)
    assert put_activity(client, h, a_log, [f.id]).status_code == 201
    assert put_activity(client, h, a_log, [f.id]).status_code == 409


def test_activity_permissions(client, world, auth, a_log, make_file):
    f = photo(make_file, world)
    assert put_activity(client, auth(world.p1), a_log).status_code == 403
    assert put_activity(client, auth(world.d2), a_log).status_code == 404          # not their salon
    assert put_activity(client, auth(world.org), a_log).status_code == 403
    aid = uuid.uuid4()
    assert put_activity(client, auth(world.d1), a_log, [f.id], aid).status_code == 201
    assert client.get(f"/activities/{aid}", headers=auth(world.p1)).status_code == 200
    assert client.get(f"/activities/{aid}", headers=auth(world.p2)).status_code == 404
    assert client.delete(f"/activities/{aid}", headers=auth(world.p1)).status_code == 403
    assert client.delete(f"/activities/{aid}", headers=auth(world.d1)).status_code == 204
    assert client.get(f"/activities/{aid}", headers=auth(world.d1)).status_code == 404


def test_activity_cannot_move_between_logs(client, world, auth, a_log):
    h = auth(world.d1)
    other = uuid.uuid4()
    put_log(client, h, world.c2, other)
    aid = uuid.uuid4()
    assert put_activity(client, h, a_log, aid=aid).status_code == 201
    assert put_activity(client, h, other, aid=aid).status_code == 422
