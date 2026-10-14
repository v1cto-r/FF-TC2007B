"""Hanging uploaded files on records: child photo, avatar, estancia logo and child documents."""
import uuid

from app.database.models import File, FilePurpose, RecordStatus, UploadState


def status_of(db, f):
    db.expire_all()
    return db.get(File, f.id).status


# --- child profile photo ------------------------------------------------------------------

def test_parent_sets_replaces_and_removes_the_child_photo(client, db, world, auth, make_file):
    h = auth(world.p1)
    one = make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1)
    two = make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1)
    url = f"/children/{world.c1.id}/photo"

    r = client.put(url, headers=h, json={"file_id": str(one.id)})
    assert r.status_code == 200 and r.json()["photo_file_id"] == str(one.id)
    assert client.get(f"/children/{world.c1.id}", headers=auth(world.d1)).json()["photo_file_id"] == str(one.id)

    assert client.put(url, headers=h, json={"file_id": str(two.id)}).json()["photo_file_id"] == str(two.id)
    assert status_of(db, one) == RecordStatus.DELETED and status_of(db, two) is None      # replaced file released

    assert client.put(url, headers=h, json={"file_id": None}).json()["photo_file_id"] is None
    assert status_of(db, two) == RecordStatus.DELETED


def test_child_photo_rules(client, world, auth, make_file):
    url = lambda c: f"/children/{c.id}/photo"
    mine = make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1)
    body = {"file_id": str(mine.id)}
    for who in (world.d1, world.admin1, world.org):
        assert client.put(url(world.c1), headers=auth(who), json=body).status_code == 403   # only the parent
    assert client.put(url(world.c1), headers=auth(world.p2), json=body).status_code == 404   # another family
    assert client.put(url(world.c2), headers=auth(world.p1), json=body).status_code == 422   # file is for c1, not c2
    wrong = make_file(world.p1, FilePurpose.MESSAGE_ATTACHMENT, child=world.c1)
    assert client.put(url(world.c1), headers=auth(world.p1), json={"file_id": str(wrong.id)}).status_code == 422
    pending = make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1, state=UploadState.PENDING)
    assert client.put(url(world.c1), headers=auth(world.p1), json={"file_id": str(pending.id)}).status_code == 422
    assert client.put(url(world.c1), headers=auth(world.p1), json={"file_id": str(uuid.uuid4())}).status_code == 422


def test_archived_child_photo_is_frozen(client, db, world, auth, make_file):
    f = make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1)
    world.c1.status = RecordStatus.ARCHIVED
    db.commit()
    assert client.put(f"/children/{world.c1.id}/photo", headers=auth(world.p1), json={"file_id": str(f.id)}).status_code == 409


# --- avatars ------------------------------------------------------------------------------

def test_every_user_manages_their_own_avatar(client, db, world, auth, make_file):
    for user in (world.org, world.admin1, world.d1, world.p1):
        h = auth(user)
        a = make_file(user, FilePurpose.USER_AVATAR, estancia=None)
        b = make_file(user, FilePurpose.USER_AVATAR, estancia=None)
        assert client.patch("/users/me", headers=h, json={"avatar_file_id": str(a.id)}).json()["avatar_file_id"] == str(a.id)
        assert client.patch("/users/me", headers=h, json={"avatar_file_id": str(b.id)}).json()["avatar_file_id"] == str(b.id)
        assert status_of(db, a) == RecordStatus.DELETED
        assert client.patch("/users/me", headers=h, json={"full_name": "Solo nombre"}).json()["avatar_file_id"] == str(b.id)  # omitted = kept
        assert client.patch("/users/me", headers=h, json={"avatar_file_id": None}).json()["avatar_file_id"] is None
        assert status_of(db, b) == RecordStatus.DELETED


def test_avatar_must_be_your_own_confirmed_avatar_file(client, world, auth, make_file):
    h = auth(world.p1)
    theirs = make_file(world.p2, FilePurpose.USER_AVATAR, estancia=None)
    wrong = make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1)
    pending = make_file(world.p1, FilePurpose.USER_AVATAR, state=UploadState.PENDING)
    for f in (theirs, wrong, pending):
        assert client.patch("/users/me", headers=h, json={"avatar_file_id": str(f.id)}).status_code == 422
    assert client.patch("/users/me", headers=h, json={"avatar_file_id": str(uuid.uuid4())}).status_code == 422


# --- estancia logo ------------------------------------------------------------------------

def test_admin_sets_the_logo_and_edits_keep_it(client, db, world, auth, make_file):
    h = auth(world.admin1)
    logo = make_file(world.admin1, FilePurpose.ESTANCIA_LOGO, estancia=world.e1)
    url = f"/estancias/{world.e1.id}"
    r = client.put(url, headers=h, json={"name": "E1", "logo_file_id": str(logo.id)})
    assert r.status_code == 200 and r.json()["logo_file_id"] == str(logo.id)
    r = client.put(url, headers=h, json={"name": "E1 renamed"})                           # logo left out: kept
    assert r.json()["name"] == "E1 renamed" and r.json()["logo_file_id"] == str(logo.id)
    assert status_of(db, logo) is None
    new = make_file(world.admin1, FilePurpose.ESTANCIA_LOGO, estancia=world.e1)
    assert client.put(url, headers=h, json={"name": "E1", "logo_file_id": str(new.id)}).json()["logo_file_id"] == str(new.id)
    assert status_of(db, logo) == RecordStatus.DELETED
    assert client.put(url, headers=h, json={"name": "E1", "logo_file_id": None}).json()["logo_file_id"] is None
    assert status_of(db, new) == RecordStatus.DELETED


def test_logo_must_belong_to_this_estancia(client, world, auth, make_file):
    other = make_file(world.admin2, FilePurpose.ESTANCIA_LOGO, estancia=world.e2)
    r = client.put(f"/estancias/{world.e1.id}", headers=auth(world.admin1), json={"name": "E1", "logo_file_id": str(other.id)})
    assert r.status_code == 422
    photo = make_file(world.admin1, FilePurpose.CHILD_PHOTO, child=world.c1)
    r = client.put(f"/estancias/{world.e1.id}", headers=auth(world.admin1), json={"name": "E1", "logo_file_id": str(photo.id)})
    assert r.status_code == 422


# --- child documents ----------------------------------------------------------------------

def doc_file(make_file, world, content_type="application/pdf", **kw):
    return make_file(world.p1, FilePurpose.CHILD_DOCUMENT, child=world.c1, content_type=content_type, **kw)


def add_doc(client, headers, world, f, title="Cartilla de vacunación", did=None, child=None):
    return client.put(f"/documents/{did or uuid.uuid4()}", headers=headers,
                      json=dict(child_id=str((child or world.c1).id), file_id=str(f.id), title=title))


def test_parent_adds_lists_and_deletes_documents(client, db, world, auth, make_file):
    f = doc_file(make_file, world)
    did = uuid.uuid4()
    r = add_doc(client, auth(world.p1), world, f, did=did)
    assert r.status_code == 201 and r.json()["file_id"] == str(f.id) and r.json()["title"] == "Cartilla de vacunación"

    listed = lambda u, c=world.c1: client.get(f"/children/{c.id}/documents", headers=auth(u))
    for who in (world.p1, world.d1, world.admin1, world.org):          # parent + staff read
        assert [d["id"] for d in listed(who).json()] == [str(did)], who
    for who in (world.p2, world.d2, world.admin2, world.p3):           # nobody else
        assert listed(who).status_code == 404, who

    r = add_doc(client, auth(world.p1), world, f, title="Cartilla 2025", did=did)    # only the title can change
    assert r.status_code == 200 and r.json()["title"] == "Cartilla 2025"

    assert client.delete(f"/documents/{did}", headers=auth(world.p1)).status_code == 204
    assert listed(world.p1).json() == [] and listed(world.d1).json() == []
    assert status_of(db, f) == RecordStatus.DELETED
    gone = client.get(f"/children/{world.c1.id}/documents?include_deleted=true", headers=auth(world.org)).json()
    assert [d["status"] for d in gone] == ["DELETED"]
    assert client.delete(f"/documents/{did}", headers=auth(world.p1)).status_code == 404


def test_only_the_parent_manages_documents(client, world, auth, make_file):
    f = doc_file(make_file, world)
    for who in (world.d1, world.admin1, world.org):
        assert add_doc(client, auth(who), world, f).status_code == 403, who
    assert add_doc(client, auth(world.p2), world, f).status_code == 404                  # not their child
    did = uuid.uuid4()
    add_doc(client, auth(world.p1), world, f, did=did)
    for who in (world.d1, world.admin1, world.org):
        assert client.delete(f"/documents/{did}", headers=auth(who)).status_code == 403, who
    assert client.delete(f"/documents/{did}", headers=auth(world.p2)).status_code == 404


def test_document_files_are_checked(client, world, auth, make_file):
    h = auth(world.p1)
    assert add_doc(client, h, world, make_file(world.p1, FilePurpose.CHILD_PHOTO, child=world.c1)).status_code == 422
    assert add_doc(client, h, world, doc_file(make_file, world, state=UploadState.PENDING)).status_code == 422
    assert add_doc(client, h, world, make_file(world.p2, FilePurpose.CHILD_DOCUMENT, child=world.c3)).status_code == 422
    other_child = make_file(world.p1, FilePurpose.CHILD_DOCUMENT, child=world.c2)
    assert add_doc(client, h, world, other_child).status_code == 422                     # file was uploaded for c2
    ok = doc_file(make_file, world, content_type="image/jpeg")                           # a scan is a valid document
    assert add_doc(client, h, world, ok).status_code == 201
    assert add_doc(client, h, world, ok).status_code == 409                              # one file, one document


def test_a_documents_file_cannot_be_swapped(client, world, auth, make_file):
    did = uuid.uuid4()
    add_doc(client, auth(world.p1), world, doc_file(make_file, world), did=did)
    r = add_doc(client, auth(world.p1), world, doc_file(make_file, world), did=did)
    assert r.status_code == 422


def test_documents_of_an_archived_child_are_frozen(client, db, world, auth, make_file):
    world.c1.status = RecordStatus.ARCHIVED
    db.commit()
    assert add_doc(client, auth(world.p1), world, doc_file(make_file, world)).status_code == 409
    assert client.get(f"/children/{world.c1.id}/documents", headers=auth(world.p1)).status_code == 200
