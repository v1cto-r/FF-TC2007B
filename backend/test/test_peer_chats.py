"""DOCENTE <-> ADMIN and ADMIN <-> ORG chats: the chat id is the docente's or the admin's own user id."""
import uuid
from contextlib import ExitStack

from app.database.models import FilePurpose, Message, RecordStatus

from .chat_helpers import connect, frame_for, ws_send

BODY = dict(content="Hola", sent_at="2026-05-01T09:30:00Z")


def body(chat, **kw):
    return dict(chat_id=str(chat.id), **{**BODY, **kw})


def chat(client, headers, owner):
    return client.get(f"/messages?chat_id={owner.id}", headers=headers)


def test_a_docente_and_their_admin_chat_and_nobody_else_hears(client, db, world, auth):
    with ExitStack() as stack:
        sock = {u.email: stack.enter_context(connect(client, auth(u)))
                for u in (world.d1, world.admin1, world.d2, world.admin2, world.org, world.p1)}
        mid = uuid.uuid4()
        sock["d1@x"].send_json(frame_for(body(world.d1, content="¿Mañana hay junta?"), mid))
        assert sock["d1@x"].receive_json()["created"] is True
        got = sock["admin1@x"].receive_json()["message"]
        assert got["chat_id"] == str(world.d1.id) and got["content"] == "¿Mañana hay junta?"
        sock["admin1@x"].send_json(frame_for(body(world.d1, content="Sí, a las 9")))
        assert sock["admin1@x"].receive_json()["type"] == "ack"
        assert sock["d1@x"].receive_json()["message"]["author_id"] == str(world.admin1.id)
        for email in ("d2@x", "admin2@x", "org@x", "p1@x"):
            sock[email].send_json({"type": "ping"})
            assert sock[email].receive_json() == {"type": "pong"}
    stored = db.get(Message, mid)
    assert stored.peer_id == world.d1.id and stored.child_id is None


def test_an_admin_and_org_chat(client, world, auth):
    with connect(client, auth(world.admin1)) as admin, connect(client, auth(world.org)) as org:
        admin.send_json(frame_for(body(world.admin1, content="Necesito material")))
        admin.receive_json()
        assert org.receive_json()["message"]["chat_id"] == str(world.admin1.id)
        org.send_json(frame_for(body(world.admin1, content="Va en camino")))
        assert org.receive_json()["type"] == "ack"
        assert admin.receive_json()["message"]["content"] == "Va en camino"
    assert [m["content"] for m in chat(client, auth(world.admin1), world.admin1).json()] == \
        ["Necesito material", "Va en camino"]


def test_who_reads_and_writes_each_peer_chat(client, world, auth):
    ws_send(client, auth(world.d1), body(world.d1))
    ws_send(client, auth(world.admin1), body(world.admin1))
    for who in (world.d1, world.admin1, world.org):                        # org reads every chat...
        assert len(chat(client, auth(who), world.d1).json()) == 1
    assert ws_send(client, auth(world.org), body(world.d1)).status_code == 403   # ...but doesn't write in a docente's
    for who in (world.d2, world.d3, world.admin2, world.p1):
        assert chat(client, auth(who), world.d1).status_code == 404
        assert ws_send(client, auth(who), body(world.d1)).status_code == 404
    for who in (world.d1, world.admin2, world.p1):
        assert chat(client, auth(who), world.admin1).status_code == 404
        assert ws_send(client, auth(who), body(world.admin1)).status_code == 404


def test_ids_that_name_no_chat_are_404(client, world, auth):
    for chat_id in (uuid.uuid4(), world.p1.id, world.org.id, world.e1.id):
        assert client.get(f"/messages?chat_id={chat_id}", headers=auth(world.org)).status_code == 404
        assert ws_send(client, auth(world.org), dict(chat_id=str(chat_id), **BODY)).status_code == 404


def test_the_admin_of_the_docente_s_estancia_changes_with_the_estancia(client, db, world, auth):
    world.e1.admin_user_id = world.admin2.id
    world.e2.admin_user_id = world.admin1.id
    db.commit()
    assert ws_send(client, auth(world.admin1), body(world.d1)).status_code == 404
    assert ws_send(client, auth(world.admin2), body(world.d1)).status_code == 201


def test_peer_chat_messages_cant_be_deleted(client, world, auth):
    mid = uuid.uuid4()
    ws_send(client, auth(world.d1), body(world.d1), mid)
    for who in (world.admin1, world.d1, world.org):
        assert client.delete(f"/messages/{mid}", headers=auth(who)).status_code == 403
    assert client.delete(f"/messages/{mid}", headers=auth(world.p1)).status_code == 404     # not their chat
    assert len(chat(client, auth(world.d1), world.d1).json()) == 1


def test_an_archived_docente_s_chat_is_read_only(client, db, world, auth):
    ws_send(client, auth(world.admin1), body(world.d1))
    world.d1.status = RecordStatus.ARCHIVED
    db.commit()
    assert ws_send(client, auth(world.admin1), body(world.d1)).status_code == 409
    assert len(chat(client, auth(world.admin1), world.d1).json()) == 1


# --- avisos -------------------------------------------------------------------------------

def test_an_aviso_carries_a_marker_line_before_the_photo(client, world, auth, make_file):
    plain = ws_send(client, auth(world.admin1), body(world.d1, content="Junta", is_aviso=True))
    assert plain.json()["content"] == "Junta\n::Aviso" and plain.json()["is_aviso"] is True
    f = make_file(world.admin1, FilePurpose.MESSAGE_ATTACHMENT, peer=world.d1)
    both = ws_send(client, auth(world.admin1), body(world.d1, content="Mira", is_aviso=True,
                                                    attachment_file_id=str(f.id)))
    assert both.json()["content"] == f"Mira\n::Aviso\n::Image /{f.id}.jpg"
    alone = ws_send(client, auth(world.d1), body(world.c1, content="", is_aviso=True))
    assert alone.json()["content"] == "::Aviso"
    assert [m["content"] for m in chat(client, auth(world.d1), world.d1).json()] == \
        ["Junta\n::Aviso", f"Mira\n::Aviso\n::Image /{f.id}.jpg"]


# --- photos -------------------------------------------------------------------------------

def test_photos_travel_in_peer_chats_and_only_their_people_see_them(client, world, auth, make_file):
    f = make_file(world.d1, FilePurpose.MESSAGE_ATTACHMENT, peer=world.d1)
    r = ws_send(client, auth(world.d1), body(world.d1, content="Foto", attachment_file_id=str(f.id)))
    assert r.status_code == 201 and r.json()["content"] == f"Foto\n::Image /{f.id}.jpg"
    for who in (world.d1, world.admin1, world.org):
        assert client.get(f"/files/{f.id}", headers=auth(who)).status_code == 200
    for who in (world.d2, world.admin2, world.p1):
        assert client.get(f"/files/{f.id}", headers=auth(who)).status_code == 404


def test_a_photo_stays_in_the_chat_it_was_uploaded_for(client, world, auth, make_file):
    child_photo = make_file(world.d1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1)
    assert ws_send(client, auth(world.d1), body(world.d1, attachment_file_id=str(child_photo.id))).status_code == 422
    peer_photo = make_file(world.d1, FilePurpose.MESSAGE_ATTACHMENT, peer=world.d1)
    assert ws_send(client, auth(world.d1), body(world.c1, attachment_file_id=str(peer_photo.id))).status_code == 422
    admin_photo = make_file(world.admin1, FilePurpose.MESSAGE_ATTACHMENT, peer=world.admin1)
    assert ws_send(client, auth(world.admin1), body(world.d1, attachment_file_id=str(admin_photo.id))).status_code == 422
