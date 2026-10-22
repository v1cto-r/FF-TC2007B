"""The live chat socket: who may connect, who receives what, and how photos and deletes travel."""
import time
import uuid
from contextlib import ExitStack
from datetime import datetime, timedelta, timezone

import jwt
import pytest
from starlette.websockets import WebSocketDisconnect

from app.config import settings
from app.database.models import FilePurpose, Message, UserRole

from .chat_helpers import connect, frame_for

BODY = dict(content="Hola", sent_at="2026-05-01T09:30:00Z")


def body(child, **kw):
    return dict(chat_id=str(child.id), **{**BODY, **kw})


def closed_with(client, headers) -> int:
    with pytest.raises(WebSocketDisconnect) as e:
        with client.websocket_connect("/ws/chat", headers=headers) as ws:
            ws.receive_json()
    return e.value.code


def token(user, minutes=5, role=None, pending=False):
    now = datetime.now(timezone.utc)
    claims = dict(sub=str(user.id), email=user.email, full_name=user.full_name, role=role or user.role.value,
                  estancia_id=None, pending=pending, typ="session", jti=str(uuid.uuid4()), iat=now,
                  exp=now + timedelta(minutes=minutes))
    return jwt.encode(claims, settings.session_secret_key, algorithm=settings.jwt_algorithm)


# --- handshake ----------------------------------------------------------------------------

def test_the_socket_needs_a_valid_session_token(client, world):
    assert closed_with(client, {}) == 4401
    assert closed_with(client, {"Authorization": "Bearer nope"}) == 4401
    assert closed_with(client, {"Authorization": f"Bearer {token(world.p1, minutes=-1)}"}) == 4401


def test_users_with_a_pending_profile_cannot_connect(client, world, make_user):
    pending = make_user("new@x", UserRole.PADRE, world.e1.id, pending=True)
    assert closed_with(client, {"Authorization": f"Bearer {token(pending, pending=True)}"}) == 4401


def test_a_socket_is_closed_when_its_token_expires(client, world):
    with connect(client, {"Authorization": f"Bearer {token(world.p1, minutes=1 / 60)}"}) as ws:   # 1 s
        with pytest.raises(WebSocketDisconnect) as e:
            ws.receive_json()
        assert e.value.code == 4401


def test_a_fresh_token_keeps_the_socket_open(client, world):
    with connect(client, {"Authorization": f"Bearer {token(world.p1, minutes=1 / 30)}"}) as ws:   # 2 s
        ws.send_json({"type": "auth", "token": token(world.p1)})
        assert ws.receive_json() == {"type": "auth_ok"}
        time.sleep(2.5)                                      # the first token is long dead by now
        ws.send_json({"type": "ping"})
        assert ws.receive_json() == {"type": "pong"}


def test_deleting_a_user_closes_their_open_sockets(client, world, auth, make_user):
    spare = make_user("kicked@x", UserRole.DOCENTE, world.e1.id)   # no salon, so it can be deleted
    with connect(client, auth(spare)) as ws:
        assert client.delete(f"/users/{spare.id}", headers=auth(world.admin1)).status_code == 204
        with pytest.raises(WebSocketDisconnect) as e:
            ws.receive_json()
        assert e.value.code == 4401
    assert closed_with(client, auth(spare)) == 4401   # and it can't come back with the same token


def test_a_token_of_another_user_is_refused(client, world, auth):
    with connect(client, auth(world.p1)) as ws:
        ws.send_json({"type": "auth", "token": token(world.d1)})
        with pytest.raises(WebSocketDisconnect) as e:
            ws.receive_json()
        assert e.value.code == 4401


def test_a_silent_socket_is_closed_but_pings_keep_it_alive(client, world, auth, monkeypatch):
    monkeypatch.setattr(settings, "chat_idle_seconds", 1.0)
    with connect(client, auth(world.p1)) as ws:
        for _ in range(3):                                   # 1.8 s in total, longer than the limit
            time.sleep(0.6)
            ws.send_json({"type": "ping"})
            assert ws.receive_json() == {"type": "pong"}
        with pytest.raises(WebSocketDisconnect) as e:        # now say nothing
            ws.receive_json()
        assert e.value.code == 4408


# --- routing ------------------------------------------------------------------------------

def test_a_message_reaches_the_child_s_people_and_nobody_else(client, db, world, auth):
    with ExitStack() as stack:
        sock = {u.email: stack.enter_context(connect(client, auth(u)))
                for u in (world.p1, world.d1, world.admin1, world.p2, world.d2, world.admin2, world.org)}
        fid = uuid.uuid4()
        sock["p1@x"].send_json(frame_for(body(world.c1, content="Llego tarde"), fid))
        ack = sock["p1@x"].receive_json()
        assert ack["type"] == "ack" and ack["created"] is True and ack["id"] == str(fid)

        for email in ("d1@x", "admin1@x"):
            got = sock[email].receive_json()
            assert got["type"] == "message" and got["message"]["content"] == "Llego tarde"
            assert got["message"]["id"] == str(fid) and got["message"]["author_id"] == str(world.p1.id)

        # Prove the others heard nothing: whatever they get next is the answer to their own ping
        for email in ("p2@x", "d2@x", "admin2@x", "org@x"):
            sock[email].send_json({"type": "ping"})
            assert sock[email].receive_json() == {"type": "pong"}
    assert db.get(Message, fid).content == "Llego tarde"


def test_the_sender_s_other_devices_get_the_message_but_not_the_device_that_sent_it(client, world, auth):
    with connect(client, auth(world.p1)) as phone, connect(client, auth(world.p1)) as tablet:
        phone.send_json(frame_for(body(world.c1)))
        assert phone.receive_json()["type"] == "ack"
        assert tablet.receive_json()["type"] == "message"
        phone.send_json({"type": "ping"})
        assert phone.receive_json() == {"type": "pong"}      # no echo of its own message in between


def test_a_retry_is_acked_but_not_delivered_twice(client, world, auth):
    mid = uuid.uuid4()
    with connect(client, auth(world.p1)) as parent, connect(client, auth(world.d1)) as docente:
        parent.send_json(frame_for(body(world.c1), mid))
        assert parent.receive_json()["created"] is True
        assert docente.receive_json()["type"] == "message"
        parent.send_json(frame_for(body(world.c1), mid))
        assert parent.receive_json()["created"] is False
        docente.send_json({"type": "ping"})
        assert docente.receive_json() == {"type": "pong"}
        parent.send_json(frame_for(body(world.c1, content="otro"), mid))
        assert parent.receive_json()["status"] == 409


def test_errors_come_back_as_frames_and_the_socket_stays_open(client, world, auth):
    with connect(client, auth(world.p1)) as ws:
        ws.send_json(frame_for(body(world.c3)))                    # someone else's child
        assert ws.receive_json()["status"] == 404
        ws.send_json(frame_for(body(world.c1, sent_at="2026-05-01T09:30:00")))   # no timezone
        assert ws.receive_json()["status"] == 422
        ws.send_json({"type": "nonsense"})
        assert ws.receive_json()["status"] == 422
        ws.send_text("not json")
        assert ws.receive_json()["status"] == 422
        ws.send_json(frame_for(body(world.c1)))
        assert ws.receive_json()["type"] == "ack"


def test_the_admin_writes_in_a_child_s_chat_and_org_only_reads_it(client, world, auth):
    with connect(client, auth(world.admin1)) as admin, connect(client, auth(world.p1)) as parent:
        admin.send_json(frame_for(body(world.c1)))
        assert admin.receive_json()["type"] == "ack"
        assert parent.receive_json()["message"]["author_id"] == str(world.admin1.id)
    with connect(client, auth(world.org)) as ws:
        ws.send_json(frame_for(body(world.c1)))
        assert ws.receive_json()["status"] == 403


def test_access_is_checked_again_on_every_message(client, db, world, auth):
    with connect(client, auth(world.d1)) as ws:
        ws.send_json(frame_for(body(world.c1)))
        assert ws.receive_json()["type"] == "ack"
        world.s1.docente_id = world.d2.id                            # the child's group changes teacher
        db.commit()
        ws.send_json(frame_for(body(world.c1)))
        assert ws.receive_json()["status"] == 404


def test_messages_are_rate_limited_per_user(client, world, auth):
    with connect(client, auth(world.p1)) as ws:
        statuses = []
        for _ in range(62):
            ws.send_json(frame_for(body(world.c1)))
            r = ws.receive_json()
            statuses.append(r.get("status", 201))
    assert statuses[:60] == [201] * 60 and statuses[60:] == [429, 429]


# --- photos -------------------------------------------------------------------------------

def test_a_photo_travels_as_an_image_marker_the_client_resolves_through_files(client, world, auth, make_file):
    f = make_file(world.p1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1)
    with connect(client, auth(world.p1)) as parent, connect(client, auth(world.d1)) as docente:
        parent.send_json(frame_for(body(world.c1, content="Mira", attachment_file_id=str(f.id))))
        assert parent.receive_json()["message"]["content"] == f"Mira\n::Image /{f.id}.jpg"
        got = docente.receive_json()["message"]
        assert got["content"] == f"Mira\n::Image /{f.id}.jpg" and got["attachment_file_id"] == str(f.id)
        parent.send_json(frame_for(body(world.c1, content="", attachment_file_id=str(make_file(
            world.p1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1).id))))
        assert parent.receive_json()["message"]["content"].startswith("::Image /")
    assert client.get(f"/files/{f.id}", headers=auth(world.d1)).status_code == 200      # what the client does next
    history = client.get(f"/messages?chat_id={world.c1.id}", headers=auth(world.d1)).json()
    assert history[0]["content"] == f"Mira\n::Image /{f.id}.jpg"


def test_a_photo_from_somewhere_else_is_refused(client, world, auth, make_file):
    theirs = make_file(world.d1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1)
    with connect(client, auth(world.p1)) as ws:
        ws.send_json(frame_for(body(world.c1, attachment_file_id=str(theirs.id))))
        assert ws.receive_json()["status"] == 422


# --- deleting -----------------------------------------------------------------------------

def test_deleting_a_message_tells_everyone_in_the_chat(client, world, auth):
    mid = uuid.uuid4()
    with connect(client, auth(world.p1)) as parent, connect(client, auth(world.d1)) as docente:
        docente.send_json(frame_for(body(world.c1), mid))
        docente.receive_json()
        parent.receive_json()
        assert client.delete(f"/messages/{mid}", headers=auth(world.p1)).status_code == 204
        expected = {"type": "deleted", "id": str(mid), "chat_id": str(world.c1.id)}
        assert parent.receive_json() == expected and docente.receive_json() == expected
