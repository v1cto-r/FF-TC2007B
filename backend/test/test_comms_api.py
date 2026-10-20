"""Notices (avisos) and the per-child chat."""
import uuid
from datetime import datetime, timezone

import pytest
from sqlalchemy import select

from .chat_helpers import ws_send
from app.database.models import AuditLog, File, FilePurpose, Message, RecordStatus

SENT = "2026-05-01T09:30:00Z"


def notice(estancia, salon=None, name="Junta", day="2026-06-01"):
    return dict(estancia_id=str(estancia.id), salon_id=str(salon.id) if salon else None, name=name,
                description="Viernes 9:00", date=day)


def put_notice(client, headers, body, nid=None):
    return client.put(f"/notices/{nid or uuid.uuid4()}", headers=headers, json=body)


# --- notices ------------------------------------------------------------------------------

def test_admin_posts_to_the_whole_estancia_or_one_salon(client, world, auth):
    h = auth(world.admin1)
    r = put_notice(client, h, notice(world.e1))
    assert r.status_code == 201 and r.json()["salon_id"] is None and r.json()["author_id"] == str(world.admin1.id)
    assert put_notice(client, h, notice(world.e1, world.s2)).status_code == 201
    assert put_notice(client, h, notice(world.e1, world.s3)).status_code == 422     # salon of another estancia
    assert put_notice(client, h, notice(world.e2)).status_code == 404               # not their estancia


def test_docente_posts_only_to_their_own_salon(client, world, auth):
    h = auth(world.d1)
    assert put_notice(client, h, notice(world.e1, world.s1)).status_code == 201
    assert put_notice(client, h, notice(world.e1, world.s2)).status_code == 404     # a colleague's salon
    assert put_notice(client, h, notice(world.e1)).status_code == 403               # whole estancia is for admins
    for who in (world.p1, world.org):
        assert put_notice(client, auth(who), notice(world.e1, world.s1)).status_code == 403


def test_notice_visibility(client, world, auth):
    h1 = auth(world.admin1)
    whole = put_notice(client, h1, notice(world.e1, name="para todos")).json()["id"]
    s1 = put_notice(client, h1, notice(world.e1, world.s1, name="solo s1")).json()["id"]
    s2 = put_notice(client, h1, notice(world.e1, world.s2, name="solo s2")).json()["id"]
    other = put_notice(client, auth(world.admin2), notice(world.e2, name="otra estancia")).json()["id"]
    ids = lambda u, q="": {n["id"] for n in client.get("/notices" + q, headers=auth(u)).json()}
    assert ids(world.p1) == {whole, s1}          # s1's parent: estancia-wide plus their child's salon
    assert ids(world.p2) == {whole, s2}
    assert ids(world.d1) == {whole, s1}
    assert ids(world.admin1) == {whole, s1, s2}
    assert ids(world.p3) == {other}
    assert ids(world.org) == {whole, s1, s2, other}
    assert ids(world.admin1, f"?salon_id={world.s2.id}") == {s2}
    assert client.get(f"/notices/{s2}", headers=auth(world.p1)).status_code == 404
    assert client.get(f"/notices/{other}", headers=auth(world.admin1)).status_code == 404


def test_only_author_or_estancia_admin_edit_and_delete_a_notice(client, world, auth):
    nid = uuid.uuid4()
    put_notice(client, auth(world.d1), notice(world.e1, world.s1), nid)
    edit = lambda u: put_notice(client, auth(u), notice(world.e1, world.s1, name="Editado"), nid).status_code
    assert edit(world.d1) == 200 and edit(world.admin1) == 200
    assert edit(world.d2) == 404 and edit(world.p1) == 403 and edit(world.admin2) == 404
    r = put_notice(client, auth(world.d1), notice(world.e1, world.s2), nid)
    assert r.status_code in (404, 422)                      # can't move to a salon that isn't theirs / another salon
    assert client.delete(f"/notices/{nid}", headers=auth(world.d2)).status_code == 404
    assert client.delete(f"/notices/{nid}", headers=auth(world.p1)).status_code == 403
    assert client.delete(f"/notices/{nid}", headers=auth(world.admin1)).status_code == 204
    assert client.get(f"/notices/{nid}", headers=auth(world.d1)).status_code == 404
    assert client.get(f"/notices/{nid}", headers=auth(world.org)).json()["status"] == "DELETED"


def test_an_admin_can_delete_a_docentes_notice_and_it_is_audited(client, db, world, auth):
    nid = uuid.uuid4()
    put_notice(client, auth(world.d1), notice(world.e1, world.s1), nid)
    client.delete(f"/notices/{nid}", headers=auth(world.admin1))
    last = db.scalars(select(AuditLog).where(AuditLog.resource_id == nid).order_by(AuditLog.created_at)).all()[-1]
    assert last.actor_id == world.admin1.id and last.changes["status"] == [None, "DELETED"]


def test_notice_rejects_bad_input(client, world, auth):
    h = auth(world.admin1)
    assert put_notice(client, h, {**notice(world.e1), "name": ""}).status_code == 422
    assert put_notice(client, h, {**notice(world.e1), "date": "mañana"}).status_code == 422


# --- messages -----------------------------------------------------------------------------

def msg(child, text="Hola", sent=SENT, **kw):
    return dict(chat_id=str(child.id), content=text, sent_at=sent, **kw)


send = ws_send   # messages go through the chat socket


def chat(client, headers, child):
    return client.get(f"/messages?chat_id={child.id}", headers=headers)


def test_parent_and_docente_chat_about_a_child(client, world, auth):
    assert send(client, auth(world.p1), msg(world.c1, "Hoy llevo a Sofi más tarde", "2026-05-01T07:00:00Z")).status_code == 201
    assert send(client, auth(world.d1), msg(world.c1, "Gracias por avisar", "2026-05-01T07:05:00Z")).status_code == 201
    r = chat(client, auth(world.p1), world.c1)
    assert [m["content"] for m in r.json()] == ["Hoy llevo a Sofi más tarde", "Gracias por avisar"]
    assert {m["author_id"] for m in r.json()} == {str(world.p1.id), str(world.d1.id)}
    assert [m["content"] for m in chat(client, auth(world.d1), world.c1).json()] == [m["content"] for m in r.json()]
    assert len(chat(client, auth(world.admin1), world.c1).json()) == 2 and len(chat(client, auth(world.org), world.c1).json()) == 2


def test_chat_is_private_to_the_child_s_people(client, world, auth):
    send(client, auth(world.p1), msg(world.c1))
    for who in (world.p2, world.d2, world.admin2, world.p3):
        assert chat(client, auth(who), world.c1).status_code == 404
    for who in (world.p2, world.d2, world.admin2, world.p3):
        assert send(client, auth(who), msg(world.c1)).status_code == 404
    assert chat(client, auth(world.p1), world.c3).status_code == 404


def test_the_estancia_admin_writes_in_a_child_s_chat_and_org_only_reads(client, world, auth):
    assert send(client, auth(world.admin1), msg(world.c1)).status_code == 201
    assert send(client, auth(world.org), msg(world.c1)).status_code == 403


def test_messages_are_never_edited_and_retries_are_harmless(client, world, auth):
    mid = uuid.uuid4()
    first = send(client, auth(world.p1), msg(world.c1, "Hola"), mid)
    assert first.status_code == 201
    again = send(client, auth(world.p1), msg(world.c1, "Hola"), mid)        # the offline queue retrying
    assert again.status_code == 200 and again.json()["id"] == first.json()["id"]
    assert len(chat(client, auth(world.p1), world.c1).json()) == 1
    assert send(client, auth(world.p1), msg(world.c1, "Hola editado"), mid).status_code == 409
    assert send(client, auth(world.d1), msg(world.c1, "Hola"), mid).status_code == 409   # someone else's id


def test_sent_at_must_carry_a_timezone(client, world, auth):
    assert send(client, auth(world.p1), msg(world.c1, sent="2026-05-01T09:30:00")).status_code == 422
    assert send(client, auth(world.p1), msg(world.c1, sent="2026-05-01T09:30:00-06:00")).status_code == 201
    assert send(client, auth(world.p1), msg(world.c1, text="x" * 20_000)).status_code == 422


def test_parent_deletes_any_message_for_both_sides(client, db, world, auth):
    mine, theirs = uuid.uuid4(), uuid.uuid4()
    send(client, auth(world.p1), msg(world.c1, "mío"), mine)
    send(client, auth(world.d1), msg(world.c1, "del docente"), theirs)
    assert client.delete(f"/messages/{theirs}", headers=auth(world.p1)).status_code == 204   # even the docente's
    assert client.delete(f"/messages/{mine}", headers=auth(world.p1)).status_code == 204
    assert chat(client, auth(world.p1), world.c1).json() == []
    assert chat(client, auth(world.d1), world.c1).json() == []                                # gone for the docente too
    assert db.get(Message, theirs).status == RecordStatus.DELETED and db.get(Message, theirs).content == "del docente"
    kept = chat(client, auth(world.org), world.c1)
    assert kept.status_code == 200 and kept.json() == []
    shown = client.get(f"/messages?chat_id={world.c1.id}&include_deleted=true", headers=auth(world.org)).json()
    assert {m["content"] for m in shown} == {"mío", "del docente"}


def test_docente_and_org_cannot_delete_messages(client, world, auth):
    mid = uuid.uuid4()
    send(client, auth(world.d1), msg(world.c1), mid)
    for who in (world.d1, world.org):
        assert client.delete(f"/messages/{mid}", headers=auth(who)).status_code == 403
    for who in (world.p2, world.admin2):                                                    # another family / estancia
        assert client.delete(f"/messages/{mid}", headers=auth(who)).status_code == 404
    assert len(chat(client, auth(world.d1), world.c1).json()) == 1


def test_the_estancia_admin_deletes_any_message_of_a_child_s_chat(client, world, auth):
    mid = uuid.uuid4()
    send(client, auth(world.p1), msg(world.c1), mid)
    assert client.delete(f"/messages/{mid}", headers=auth(world.admin1)).status_code == 204
    assert chat(client, auth(world.p1), world.c1).json() == []


def test_archived_child_chat_is_read_only(client, db, world, auth):
    mid = uuid.uuid4()
    send(client, auth(world.p1), msg(world.c1), mid)
    world.c1.status = RecordStatus.ARCHIVED
    db.commit()
    assert send(client, auth(world.p1), msg(world.c1, "otro")).status_code == 409
    assert len(chat(client, auth(world.p1), world.c1).json()) == 1


# --- photos in the chat -------------------------------------------------------------------

def test_photo_attachment_travels_with_the_message(client, db, world, auth, make_file):
    f = make_file(world.p1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1)
    r = send(client, auth(world.p1), msg(world.c1, "Mira", attachment_file_id=str(f.id)))
    assert r.status_code == 201 and r.json()["attachment_file_id"] == str(f.id)
    assert chat(client, auth(world.d1), world.c1).json()[0]["attachment_file_id"] == str(f.id)


def test_attachment_rules(client, world, auth, make_file):
    ok = lambda who, **kw: str(make_file(who, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1, **kw).id)
    h = auth(world.p1)
    assert send(client, h, msg(world.c1, attachment_file_id=ok(world.d1))).status_code == 422          # someone else's upload
    assert send(client, h, msg(world.c1, attachment_file_id=str(uuid.uuid4()))).status_code == 422
    other_child = make_file(world.p1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c2)
    assert send(client, h, msg(world.c1, attachment_file_id=str(other_child.id))).status_code == 422   # another child's chat
    wrong = make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1)
    assert send(client, h, msg(world.c1, attachment_file_id=str(wrong.id))).status_code == 422
    used = ok(world.p1)
    assert send(client, h, msg(world.c1, attachment_file_id=used)).status_code == 201
    assert send(client, h, msg(world.c1, attachment_file_id=used)).status_code == 409                  # one photo, one message


def test_deleting_a_message_hides_its_photo(client, db, world, auth, make_file):
    f = make_file(world.p1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1)
    mid = uuid.uuid4()
    send(client, auth(world.p1), msg(world.c1, attachment_file_id=str(f.id)), mid)
    assert client.get(f"/files/{f.id}", headers=auth(world.d1)).status_code == 200
    client.delete(f"/messages/{mid}", headers=auth(world.p1))
    db.expire_all()
    assert db.get(File, f.id).status == RecordStatus.DELETED
    assert client.get(f"/files/{f.id}", headers=auth(world.d1)).status_code == 404       # hidden from staff and parent...
    assert client.get(f"/files/{f.id}", headers=auth(world.p1)).status_code == 404
    assert client.get(f"/files/{f.id}", headers=auth(world.org)).status_code == 200      # ...ORG keeps access for audit


def test_since_returns_only_what_the_server_stored_after_it(client, world, auth):
    first = send(client, auth(world.p1), msg(world.c1, "uno")).json()
    send(client, auth(world.d1), msg(world.c1, "dos"))
    send(client, auth(world.d1), msg(world.c1, "tres", sent="2026-04-01T00:00:00Z"))   # old sent_at, stored last
    got = client.get("/messages", params={"chat_id": str(world.c1.id), "since": first["created_at"]}, headers=auth(world.p1))
    assert [m["content"] for m in got.json()] == ["tres", "dos"]                          # still ordered by sent_at
    assert chat(client, auth(world.p1), world.c1).json()[-1]["content"] == "dos"
    assert client.get(f"/messages?chat_id={world.c1.id}&since=2026-05-01T09:30:00", headers=auth(world.p1)).status_code == 422
