"""`role_actions` lives in the database, so someone can grant too much. These rules are also enforced in code:
even a role that was handed every action can't do what only parents / the child's docente / the estancia's
admin may do."""
import uuid

import pytest

from .chat_helpers import ws_send
from app.database.models import FilePurpose, RoleAction, UserRole

MSG = dict(content="hola", sent_at="2026-05-01T09:00:00Z")


@pytest.fixture
def everything_granted(db):
    db.add_all(RoleAction(role=r, action="*") for r in (UserRole.ORG, UserRole.ADMIN, UserRole.DOCENTE))
    db.commit()


def test_only_the_parent_and_the_admin_delete_messages(client, world, auth, everything_granted):
    mid = uuid.uuid4()
    ws_send(client, auth(world.p1), dict(chat_id=str(world.c1.id), **MSG), mid)
    for who in (world.d1, world.org):
        assert client.delete(f"/messages/{mid}", headers=auth(who)).status_code == 403, who.email


def test_org_doesnt_write_in_a_child_s_chat(client, world, auth, everything_granted):
    r = ws_send(client, auth(world.org), dict(chat_id=str(world.c1.id), **MSG))
    assert r.status_code == 403


def test_peer_chat_messages_are_never_deleted(client, world, auth, everything_granted):
    mid = uuid.uuid4()
    ws_send(client, auth(world.d1), dict(chat_id=str(world.d1.id), **MSG), mid)
    for who in (world.admin1, world.org, world.d1):
        assert client.delete(f"/messages/{mid}", headers=auth(who)).status_code == 403, who.email


def test_only_the_parent_sets_photo_and_manages_documents(client, world, auth, make_file, everything_granted):
    photo = make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1)
    doc = make_file(world.p1, FilePurpose.CHILD_DOCUMENT, child=world.c1, content_type="application/pdf")
    for who in (world.d1, world.admin1, world.org):
        h = auth(who)
        assert client.put(f"/children/{world.c1.id}/photo", headers=h, json={"file_id": str(photo.id)}).status_code == 403
        body = dict(child_id=str(world.c1.id), file_id=str(doc.id), title="x")
        assert client.put(f"/documents/{uuid.uuid4()}", headers=h, json=body).status_code == 403
    did = uuid.uuid4()
    client.put(f"/documents/{did}", headers=auth(world.p1), json=dict(child_id=str(world.c1.id), file_id=str(doc.id), title="x"))
    for who in (world.d1, world.admin1, world.org):
        assert client.delete(f"/documents/{did}", headers=auth(who)).status_code == 403


@pytest.mark.parametrize("who,purpose,target", [
    ("d1", "CHILD_PHOTO", "c1"), ("d1", "CHILD_DOCUMENT", "c1"), ("d1", "ESTANCIA_LOGO", "e1"),
    ("admin1", "ACTIVITY_IMAGE", "c1"), ("admin1", "CHILD_PHOTO", "c1"),
    ("org", "ACTIVITY_IMAGE", "c1"), ("org", "MESSAGE_ATTACHMENT", "c1"), ("org", "CHILD_DOCUMENT", "c1"), ("org", "ESTANCIA_LOGO", "e1"),
])
def test_upload_purposes_are_tied_to_roles(client, world, auth, everything_granted, who, purpose, target):
    ct = "application/pdf" if purpose == "CHILD_DOCUMENT" else "image/jpeg"
    r = client.post("/files", headers=auth(getattr(world, who)), json=dict(
        id=str(uuid.uuid4()), purpose=purpose, target_id=str(getattr(world, target).id), content_type=ct, size=10))
    assert r.status_code == 403


def test_only_admins_and_docentes_post_notices(client, world, auth, everything_granted):
    body = dict(estancia_id=str(world.e1.id), salon_id=None, name="x", description="y", date="2026-01-01")
    assert client.put(f"/notices/{uuid.uuid4()}", headers=auth(world.org), json=body).status_code == 403
    assert client.put(f"/notices/{uuid.uuid4()}", headers=auth(world.d1), json=body).status_code == 403   # whole estancia: admin only
    assert client.put(f"/notices/{uuid.uuid4()}", headers=auth(world.admin1), json=body).status_code == 201


def test_only_org_creates_estancias_and_changes_admins(client, world, auth, everything_granted):
    assert client.put(f"/estancias/{uuid.uuid4()}", headers=auth(world.admin1),
                      json=dict(name="x", admin_user_id=str(world.admin1.id))).status_code == 403
    assert client.put(f"/estancias/{world.e1.id}", headers=auth(world.admin1),
                      json=dict(name="E1", admin_user_id=str(world.admin2.id))).status_code == 403


def test_scopes_still_apply_with_every_action_granted(client, world, auth, everything_granted):
    """Granting actions never widens WHICH rows a user can reach."""
    assert client.get(f"/children/{world.c4.id}", headers=auth(world.admin1)).status_code == 404
    assert client.get(f"/children/{world.c3.id}", headers=auth(world.d1)).status_code == 404
    assert client.get(f"/estancias/{world.e2.id}", headers=auth(world.admin1)).status_code == 404
    assert client.get(f"/users/{world.d3.id}", headers=auth(world.admin1)).status_code == 404
