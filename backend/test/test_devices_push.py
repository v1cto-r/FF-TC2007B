"""Push notifications: registering devices, and who gets a push when a chat message arrives."""
import uuid
from contextlib import nullcontext

import pytest
from sqlalchemy import select

from app.chat.hub import set_push
from app.database.models import DeviceToken, RecordStatus
from app.push.service import PushService

from .chat_helpers import connect, frame_for

BODY = dict(content="Hola", sent_at="2026-05-01T09:30:00Z")


class FakeSender:
    def __init__(self):
        self.calls, self.dead, self.fail = [], [], False

    def send(self, tokens, data):
        if self.fail:
            raise RuntimeError("FCM is down")
        self.calls.append((sorted(tokens), data))
        return [t for t in tokens if t in self.dead]


@pytest.fixture
def sender(client, db):
    fake = FakeSender()
    set_push(PushService(lambda: nullcontext(db), fake, inline=True))
    yield fake
    set_push(None)


def token(name):
    return f"tok-{name}-" + "x" * 30


def register(client, headers, name, device=None):
    return client.put(f"/devices/{device or uuid.uuid4()}", headers=headers, json={"token": token(name)})


def message(child):
    return frame_for(dict(chat_id=str(child.id), **BODY))


def send_and_settle(ws, frame):
    """Sends a frame and waits until the server is done with it (pushes included): the next answer is the pong."""
    ws.send_json(frame)
    ack = ws.receive_json()
    ws.send_json({"type": "ping"})
    assert ws.receive_json() == {"type": "pong"}
    return ack


# --- registering ---------------------------------------------------------------------------

def test_a_device_registers_refreshes_and_unregisters(client, db, world, auth):
    h, dev = auth(world.p1), uuid.uuid4()
    r = register(client, h, "a", dev)
    assert r.status_code == 200 and r.json()["id"] == str(dev) and r.json()["platform"] == "ANDROID"
    assert register(client, h, "b", dev).status_code == 200                     # Firebase handed out a new token
    assert db.scalars(select(DeviceToken.token)).all() == [token("b")]
    assert client.delete(f"/devices/{dev}", headers=h).status_code == 204
    assert db.scalars(select(DeviceToken)).all() == []
    assert client.delete(f"/devices/{dev}", headers=h).status_code == 404


def test_devices_belong_to_their_user(client, db, world, auth):
    dev = uuid.uuid4()
    register(client, auth(world.p1), "a", dev)
    assert register(client, auth(world.p2), "b", dev).status_code == 404         # someone else's id
    assert client.delete(f"/devices/{dev}", headers=auth(world.p2)).status_code == 404
    assert client.put(f"/devices/{dev}", json={"token": token("a")}).status_code == 401
    assert client.put(f"/devices/{uuid.uuid4()}", headers=auth(world.p1), json={"token": "short"}).status_code == 422


def test_a_token_moves_to_whoever_registers_it_last(client, db, world, auth):
    register(client, auth(world.p1), "shared")
    register(client, auth(world.d1), "shared")                                   # same phone, another login
    rows = db.scalars(select(DeviceToken)).all()
    assert [r.user_id for r in rows] == [world.d1.id]


def test_archiving_a_user_forgets_their_devices(client, db, world, auth):
    register(client, auth(world.p1), "p1")
    register(client, auth(world.p2), "p2")
    assert client.post(f"/users/{world.p1.id}/archive", headers=auth(world.admin1)).status_code == 200
    assert db.scalars(select(DeviceToken.user_id)).all() == [world.p2.id]


# --- who gets a push -----------------------------------------------------------------------

def test_an_offline_parent_gets_a_push_with_ids_only(client, world, auth, sender):
    register(client, auth(world.p1), "p1")
    with connect(client, auth(world.d1)) as docente:
        ack = send_and_settle(docente, message(world.c1))
    assert sender.calls == [([token("p1")], {"type": "message", "chat_id": str(world.c1.id),
                                              "message_id": ack["id"]})]


def test_nobody_else_is_pushed(client, db, world, auth, sender):
    for name, user in (("p1", world.p1), ("d1", world.d1), ("admin1", world.admin1), ("p2", world.p2),
                       ("d2", world.d2), ("org", world.org)):
        register(client, auth(user), name)
    with connect(client, auth(world.d1)) as docente:                             # the author, with a token of their own
        send_and_settle(docente, message(world.c1))
    # p1 (parent) and admin1 (admin) take part in the chat; the rest of the families, other docentes and ORG do not
    assert sender.calls == [(sorted([token("p1"), token("admin1")]), sender.calls[0][1])]


def test_peer_chats_push_their_people(client, world, auth, sender):
    for name, user in (("admin1", world.admin1), ("org", world.org), ("d2", world.d2), ("admin2", world.admin2)):
        register(client, auth(user), name)
    with connect(client, auth(world.d1)) as docente:                             # d1 <-> admin1
        ack = send_and_settle(docente, message(world.d1))
    assert sender.calls == [([token("admin1")], {"type": "message", "chat_id": str(world.d1.id),
                                                 "message_id": ack["id"]})]
    with connect(client, auth(world.admin2)) as admin:                           # admin2 <-> every ORG user
        send_and_settle(admin, message(world.admin2))
    assert sender.calls[1][0] == [token("org")]


def test_a_user_with_the_socket_open_is_not_pushed(client, world, auth, sender):
    register(client, auth(world.p1), "p1")
    with connect(client, auth(world.p1)) as parent, connect(client, auth(world.d1)) as docente:
        send_and_settle(docente, message(world.c1))
        parent.receive_json()                                                    # got it live instead
    assert sender.calls == []


def test_archived_users_are_not_pushed(client, db, world, auth, sender):
    register(client, auth(world.p1), "p1")
    world.p1.status = RecordStatus.ARCHIVED
    db.commit()
    with connect(client, auth(world.d1)) as docente:
        send_and_settle(docente, message(world.c1))
    assert sender.calls == []


def test_dead_tokens_are_forgotten(client, db, world, auth, sender):
    register(client, auth(world.p1), "old")
    register(client, auth(world.p1), "new")
    sender.dead = [token("old")]
    with connect(client, auth(world.d1)) as docente:
        send_and_settle(docente, message(world.c1))
    assert db.scalars(select(DeviceToken.token)).all() == [token("new")]


def test_a_failing_push_does_not_break_the_chat(client, db, world, auth, sender):
    register(client, auth(world.p1), "p1")
    sender.fail = True
    with connect(client, auth(world.d1)) as docente:
        assert send_and_settle(docente, message(world.c1))["type"] == "ack"
        assert send_and_settle(docente, message(world.c1))["type"] == "ack"      # the socket is still fine


def test_without_firebase_credentials_nothing_happens(client, world, auth):
    set_push(None)
    register(client, auth(world.p1), "p1")
    with connect(client, auth(world.d1)) as docente:
        assert send_and_settle(docente, message(world.c1))["type"] == "ack"
