"""The file flow: ask for a signed URL, PUT the bytes, confirm, download."""
import uuid
from datetime import timedelta
from urllib.parse import parse_qs, urlparse

import pytest
from sqlalchemy import select

from app.database.models import AuditLog, File, FilePurpose, RecordStatus, UploadState, UserRole, _now
from app.storage.magic import DOCX
from app.storage.rules import expire_pending

JPEG = b"\xff\xd8\xff\xe0" + b"0" * 60
PNG = b"\x89PNG\r\n\x1a\n" + b"0" * 60
WEBP = b"RIFF\x24\x00\x00\x00WEBPVP8 " + b"0" * 40
PDF = b"%PDF-1.7\n" + b"0" * 60
ZIP = b"PK\x03\x04" + b"0" * 60


def ask(client, headers, purpose, target, content_type="image/jpeg", size=64, fid=None, **kw):
    body = dict(id=str(fid or uuid.uuid4()), purpose=purpose, content_type=content_type, size=size, **kw)
    if target is not None:
        body["target_id"] = str(target.id)
    return client.post("/files", headers=headers, json=body)


def upload(client, s3, headers, purpose, target, data=JPEG, content_type="image/jpeg"):
    """The whole happy path; returns the confirmed file."""
    r = ask(client, headers, purpose, target, content_type, len(data))
    assert r.status_code == 201, r.text
    f = r.json()["file"]
    s3.put(*bucket_key(client, f["id"]), data, content_type)
    done = client.post(f"/files/{f['id']}/complete", headers=headers)
    assert done.status_code == 200, done.text
    return done.json()


def bucket_key(client, file_id):
    from app.database.connection import get_db
    from app.main import app
    db = next(app.dependency_overrides[get_db]())
    f = db.get(File, uuid.UUID(file_id))
    return f.bucket, f.key


# --- the happy path -----------------------------------------------------------------------

def test_docente_uploads_an_activity_photo_end_to_end(client, db, s3, world, auth):
    h = auth(world.d1)
    r = ask(client, h, "ACTIVITY_IMAGE", world.c1, size=len(JPEG))
    assert r.status_code == 201
    ticket = r.json()
    f = ticket["file"]
    assert f["upload_state"] == "PENDING" and f["child_id"] == str(world.c1.id) and f["uploaded_by"] == str(world.d1.id)
    assert f["estancia_id"] == str(world.e1.id) and f["kind"] == "image"

    up = ticket["upload"]
    assert up["method"] == "PUT" and up["headers"] == {"Content-Type": "image/jpeg", "Content-Length": str(len(JPEG))}
    u = urlparse(up["url"])
    assert u.path == f"/images/activity_image/{world.c1.id}/{f['id']}.jpg"           # bucket / purpose / child / id.ext
    q = parse_qs(u.query)
    assert q["X-Amz-Expires"] == ["300"] and "X-Amz-Signature" in q
    assert "content-length" in q["X-Amz-SignedHeaders"][0] or "content-type" in q["X-Amz-SignedHeaders"][0]

    assert client.post(f"/files/{f['id']}/complete", headers=h).status_code == 409    # nothing uploaded yet
    assert client.get(f"/files/{f['id']}", headers=h).status_code == 404               # pending files are not served

    s3.put("images", f"activity_image/{world.c1.id}/{f['id']}.jpg", JPEG, "image/jpeg")
    done = client.post(f"/files/{f['id']}/complete", headers=h)
    assert done.status_code == 200 and done.json()["upload_state"] == "READY"
    assert client.post(f"/files/{f['id']}/complete", headers=h).status_code == 200      # confirming twice is fine

    dl = client.get(f"/files/{f['id']}", headers=auth(world.p1)).json()                 # the child's parent can see it
    d = urlparse(dl["url"])
    assert d.path == u.path and parse_qs(d.query)["X-Amz-Expires"] == ["900"]
    assert dl["file"]["id"] == f["id"]


@pytest.mark.parametrize("who,purpose,content_type,data", [
    ("p1", "CHILD_PHOTO", "image/png", PNG),
    ("p1", "MESSAGE_ATTACHMENT", "image/webp", WEBP),
    ("d1", "MESSAGE_ATTACHMENT", "image/jpeg", JPEG),
    ("p1", "CHILD_DOCUMENT", "application/pdf", PDF),
    ("p1", "CHILD_DOCUMENT", DOCX, ZIP),
    ("p1", "CHILD_DOCUMENT", "image/jpeg", JPEG),         # a scan or a photo of a paper
    ("d1", "PICKUP_ID_PHOTO", "image/jpeg", JPEG),
])
def test_each_purpose_accepts_its_types(client, s3, world, auth, who, purpose, content_type, data):
    f = upload(client, s3, auth(getattr(world, who)), purpose, world.c1, data, content_type)
    assert f["upload_state"] == "READY" and f["purpose"] == purpose


# --- type, size and content checks --------------------------------------------------------

@pytest.mark.parametrize("purpose,content_type,size,why", [
    ("ACTIVITY_IMAGE", "application/pdf", 100, "a pdf is not a photo"),
    ("CHILD_PHOTO", "image/gif", 100, "gif is not accepted"),
    ("CHILD_PHOTO", "text/html", 100, "html"),
    ("MESSAGE_ATTACHMENT", DOCX, 100, "documents don't go in the chat"),
    ("ACTIVITY_IMAGE", "image/jpeg", 5 * 1024 * 1024 + 1, "over the image limit"),
    ("CHILD_DOCUMENT", "application/pdf", 10 * 1024 * 1024 + 1, "over the file limit"),
    ("CHILD_DOCUMENT", "image/jpeg", 5 * 1024 * 1024 + 1, "a scan counts as an image"),
])
def test_wrong_type_or_size_is_refused_before_anything_is_stored(client, db, world, auth, purpose, content_type, size, why):
    who = world.d1 if purpose == "ACTIVITY_IMAGE" else world.p1
    r = ask(client, auth(who), purpose, world.c1, content_type, size)
    assert r.status_code == 422, why
    assert db.scalar(select(File.id)) is None


def test_non_positive_size_is_refused(client, world, auth):
    assert ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, size=0).status_code == 422
    assert ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, size=-5).status_code == 422


def test_limits_are_accepted_exactly(client, world, auth):
    assert ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, size=5 * 1024 * 1024).status_code == 201
    assert ask(client, auth(world.p1), "CHILD_DOCUMENT", world.c1, "application/pdf", 10 * 1024 * 1024).status_code == 201


@pytest.mark.parametrize("what", ["wrong bytes", "wrong size", "wrong type"])
def test_a_lying_upload_is_rejected_and_never_served(client, db, s3, world, auth, what):
    h = auth(world.d1)
    f = ask(client, h, "ACTIVITY_IMAGE", world.c1, "image/jpeg", len(JPEG)).json()["file"]
    bucket, key = bucket_key(client, f["id"])
    {"wrong bytes": lambda: s3.put(bucket, key, PDF + b"0" * (len(JPEG) - len(PDF)), "image/jpeg"),   # a pdf named .jpg
     "wrong size": lambda: s3.put(bucket, key, JPEG + b"extra", "image/jpeg"),
     "wrong type": lambda: s3.put(bucket, key, JPEG, "text/html")}[what]()
    assert client.post(f"/files/{f['id']}/complete", headers=h).status_code == 422
    db.expire_all()
    assert db.get(File, uuid.UUID(f["id"])).status == RecordStatus.DELETED
    assert client.get(f"/files/{f['id']}", headers=auth(world.org)).status_code == 404       # never READY


@pytest.mark.parametrize("ct,data", [("image/jpeg", PNG), ("image/png", JPEG), ("image/webp", JPEG),
                                     ("application/pdf", ZIP), (DOCX, PDF)])
def test_every_type_is_checked_against_its_magic_bytes(client, s3, world, auth, ct, data):
    h = auth(world.p1)
    f = ask(client, h, "CHILD_DOCUMENT", world.c1, ct, len(data)).json()["file"]
    s3.put(*bucket_key(client, f["id"]), data, ct)
    assert client.post(f"/files/{f['id']}/complete", headers=h).status_code == 422


# --- who may upload what ------------------------------------------------------------------

@pytest.mark.parametrize("who,purpose,target,expected", [
    ("d1", "ACTIVITY_IMAGE", "c1", 201), ("d1", "ACTIVITY_IMAGE", "c2", 201),
    ("d1", "ACTIVITY_IMAGE", "c3", 404),        # another salon
    ("d1", "ACTIVITY_IMAGE", "c4", 404),        # another estancia
    ("p1", "ACTIVITY_IMAGE", "c1", 403), ("admin1", "ACTIVITY_IMAGE", "c1", 403), ("org", "ACTIVITY_IMAGE", "c1", 403),
    ("p1", "CHILD_PHOTO", "c1", 201), ("p1", "CHILD_PHOTO", "c3", 404), ("p1", "CHILD_PHOTO", "c4", 404),
    ("d1", "CHILD_PHOTO", "c1", 403), ("admin1", "CHILD_PHOTO", "c1", 403), ("org", "CHILD_PHOTO", "c1", 403),
    ("p1", "CHILD_DOCUMENT", "c1", 201), ("p2", "CHILD_DOCUMENT", "c1", 404),
    ("d1", "CHILD_DOCUMENT", "c1", 403), ("admin1", "CHILD_DOCUMENT", "c1", 403), ("org", "CHILD_DOCUMENT", "c1", 403),
    ("p1", "MESSAGE_ATTACHMENT", "c1", 201), ("d1", "MESSAGE_ATTACHMENT", "c1", 201),
    ("d2", "MESSAGE_ATTACHMENT", "c1", 404), ("admin1", "MESSAGE_ATTACHMENT", "c1", 201),
    ("org", "MESSAGE_ATTACHMENT", "c1", 403), ("admin2", "MESSAGE_ATTACHMENT", "c1", 404),
    ("d1", "MESSAGE_ATTACHMENT", "d1", 201), ("admin1", "MESSAGE_ATTACHMENT", "d1", 201),
    ("admin2", "MESSAGE_ATTACHMENT", "d1", 404), ("d2", "MESSAGE_ATTACHMENT", "d1", 404),
    ("admin1", "MESSAGE_ATTACHMENT", "admin1", 201), ("org", "MESSAGE_ATTACHMENT", "admin1", 201),
    ("d1", "MESSAGE_ATTACHMENT", "admin1", 404),
    ("admin1", "ESTANCIA_LOGO", "e1", 201), ("admin1", "ESTANCIA_LOGO", "e2", 404),
    ("admin2", "ESTANCIA_LOGO", "e1", 404), ("org", "ESTANCIA_LOGO", "e1", 403),
    ("d1", "ESTANCIA_LOGO", "e1", 403), ("p1", "ESTANCIA_LOGO", "e1", 403),
    ("d1", "PICKUP_ID_PHOTO", "c1", 201), ("d2", "PICKUP_ID_PHOTO", "c1", 404),   # the teacher at check-out
    ("p1", "PICKUP_ID_PHOTO", "c1", 403), ("admin1", "PICKUP_ID_PHOTO", "c1", 403),
])
def test_upload_permissions(client, world, auth, who, purpose, target, expected):
    content_type = "application/pdf" if purpose == "CHILD_DOCUMENT" else "image/jpeg"
    r = ask(client, auth(getattr(world, who)), purpose, getattr(world, target), content_type)
    assert r.status_code == expected, r.text


def test_everyone_may_upload_their_own_avatar_and_only_theirs(client, world, auth):
    for u in (world.org, world.admin1, world.d1, world.p1):
        assert ask(client, auth(u), "USER_AVATAR", None).status_code == 201
        assert ask(client, auth(u), "USER_AVATAR", u).status_code == 201
    assert ask(client, auth(world.p1), "USER_AVATAR", world.p2).status_code == 403


def test_target_is_required_for_child_and_estancia_files(client, world, auth):
    assert ask(client, auth(world.p1), "CHILD_PHOTO", None).status_code == 422
    assert ask(client, auth(world.admin1), "ESTANCIA_LOGO", None).status_code == 422


def test_uploads_for_an_archived_child_are_refused(client, db, world, auth):
    world.c1.status = RecordStatus.ARCHIVED
    db.commit()
    assert ask(client, auth(world.p1), "CHILD_PHOTO", world.c1).status_code == 409


def test_unknown_purpose_and_junk_are_refused(client, world, auth):
    h = auth(world.p1)
    assert client.post("/files", headers=h, json={"id": str(uuid.uuid4()), "purpose": "HACK", "content_type": "image/jpeg", "size": 5}).status_code == 422
    assert client.post("/files", headers=h, json={"purpose": "CHILD_PHOTO"}).status_code == 422
    assert client.post("/files").status_code == 401


# --- retries ------------------------------------------------------------------------------

def test_asking_again_with_the_same_id_returns_a_fresh_url(client, world, auth):
    h, fid = auth(world.d1), uuid.uuid4()
    first = ask(client, h, "ACTIVITY_IMAGE", world.c1, fid=fid)
    again = ask(client, h, "ACTIVITY_IMAGE", world.c1, fid=fid)
    assert first.status_code == 201 and again.status_code == 200
    assert again.json()["file"]["id"] == first.json()["file"]["id"]
    assert urlparse(again.json()["upload"]["url"]).path == urlparse(first.json()["upload"]["url"]).path


def test_an_id_cannot_be_reused_for_something_else(client, s3, world, auth):
    fid = uuid.uuid4()
    assert ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, fid=fid, size=64).status_code == 201
    assert ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, fid=fid, size=65).status_code == 409   # different size
    assert ask(client, auth(world.p1), "CHILD_PHOTO", world.c1, fid=fid, size=64).status_code == 409     # someone else's id
    ready = upload(client, s3, auth(world.d1), "ACTIVITY_IMAGE", world.c1)
    assert ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, fid=ready["id"], size=ready["size"]).status_code == 409


def test_only_the_uploader_can_confirm(client, s3, world, auth):
    f = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, size=len(JPEG)).json()["file"]
    s3.put(*bucket_key(client, f["id"]), JPEG, "image/jpeg")
    assert client.post(f"/files/{f['id']}/complete", headers=auth(world.d2)).status_code == 404
    assert client.post(f"/files/{f['id']}/complete", headers=auth(world.admin1)).status_code == 404
    assert client.post(f"/files/{uuid.uuid4()}/complete", headers=auth(world.d1)).status_code == 404


# --- rate limits --------------------------------------------------------------------------

def test_upload_rate_limit_per_user(client, world, auth):
    client.app.state.storage.policy.rate_limits.upload.per_minute = 3
    h = auth(world.d1)
    assert [ask(client, h, "ACTIVITY_IMAGE", world.c1).status_code for _ in range(3)] == [201] * 3
    blocked = ask(client, h, "ACTIVITY_IMAGE", world.c1)
    assert blocked.status_code == 429 and int(blocked.headers["Retry-After"]) >= 1
    assert ask(client, auth(world.d2), "ACTIVITY_IMAGE", world.c3).status_code == 201     # other users unaffected


def test_daily_upload_limit(client, world, auth):
    client.app.state.storage.policy.rate_limits.upload.per_day = 2
    h = auth(world.d1)
    assert [ask(client, h, "ACTIVITY_IMAGE", world.c1).status_code for _ in range(3)] == [201, 201, 429]


def test_refused_requests_do_not_use_up_the_quota(client, world, auth):
    client.app.state.storage.policy.rate_limits.upload.per_minute = 2
    h = auth(world.p1)
    for _ in range(6):
        assert ask(client, h, "ACTIVITY_IMAGE", world.c1).status_code == 403                 # not allowed: free
        assert ask(client, h, "CHILD_PHOTO", world.c1, "image/gif").status_code == 422        # bad type: free
        assert ask(client, h, "CHILD_PHOTO", world.c3).status_code == 404                     # not their child: free
    assert [ask(client, h, "CHILD_PHOTO", world.c1).status_code for _ in range(3)] == [201, 201, 429]


def test_download_rate_limit(client, s3, world, auth):
    f = upload(client, s3, auth(world.p1), "CHILD_PHOTO", world.c1, PNG, "image/png")
    client.app.state.storage.policy.rate_limits.download.per_minute = 2
    h = auth(world.p1)
    assert [client.get(f"/files/{f['id']}", headers=h).status_code for _ in range(3)] == [200, 200, 429]
    assert client.get(f"/files/{f['id']}", headers=auth(world.d1)).status_code == 200


def test_storage_outage_is_a_503_not_a_crash(client, s3, world, auth, monkeypatch):
    from botocore.exceptions import EndpointConnectionError
    f = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1).json()["file"]
    monkeypatch.setattr(s3, "head_object", lambda **kw: (_ for _ in ()).throw(EndpointConnectionError(endpoint_url="x")))
    assert client.post(f"/files/{f['id']}/complete", headers=auth(world.d1)).status_code == 503


# --- unconfirmed uploads expire -----------------------------------------------------------

def test_unconfirmed_uploads_expire_and_it_is_audited_as_the_system(client, db, world, auth):
    stale = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1).json()["file"]["id"]
    fresh = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1).json()["file"]["id"]
    row = db.get(File, uuid.UUID(stale))
    row.created_at = _now() - timedelta(hours=1)
    db.commit()
    assert expire_pending(db, 300) == 1
    db.expire_all()
    assert db.get(File, uuid.UUID(stale)).status == RecordStatus.DELETED
    assert db.get(File, uuid.UUID(fresh)).status is None
    entry = db.scalars(select(AuditLog).where(AuditLog.resource_id == uuid.UUID(stale)).order_by(AuditLog.created_at)).all()[-1]
    assert entry.actor_id is None and entry.changes["status"] == [None, "DELETED"]
    assert expire_pending(db, 300) == 0


def test_confirmed_files_never_expire(client, db, s3, world, auth):
    f = upload(client, s3, auth(world.d1), "ACTIVITY_IMAGE", world.c1)
    row = db.get(File, uuid.UUID(f["id"]))
    row.created_at = _now() - timedelta(days=30)
    db.commit()
    assert expire_pending(db, 300) == 0


# --- downloads ----------------------------------------------------------------------------

@pytest.mark.parametrize("who,expected", [
    ("p1", 200), ("d1", 200), ("admin1", 200), ("org", 200),
    ("p2", 404), ("d2", 404), ("p3", 404), ("d3", 404), ("admin2", 404),
])
def test_who_can_download_a_child_photo(client, s3, world, auth, who, expected):
    f = upload(client, s3, auth(world.d1), "ACTIVITY_IMAGE", world.c1)
    assert client.get(f"/files/{f['id']}", headers=auth(getattr(world, who))).status_code == expected


@pytest.mark.parametrize("who,expected", [("p1", 200), ("d1", 200), ("admin1", 200), ("org", 200), ("d2", 404), ("p2", 404), ("admin2", 404)])
def test_who_can_download_a_child_document(client, s3, world, auth, who, expected):
    f = upload(client, s3, auth(world.p1), "CHILD_DOCUMENT", world.c1, PDF, "application/pdf")
    assert client.get(f"/files/{f['id']}", headers=auth(getattr(world, who))).status_code == expected


def test_estancia_logo_is_visible_to_its_people_only(client, s3, world, auth):
    f = upload(client, s3, auth(world.admin1), "ESTANCIA_LOGO", world.e1)
    for who in ("admin1", "d1", "p1", "org"):
        assert client.get(f"/files/{f['id']}", headers=auth(getattr(world, who))).status_code == 200, who
    for who in ("admin2", "d3", "p3"):
        assert client.get(f"/files/{f['id']}", headers=auth(getattr(world, who))).status_code == 404, who


def test_avatars_are_visible_in_the_estancia_and_to_org(client, s3, world, auth):
    f = upload(client, s3, auth(world.p1), "USER_AVATAR", None)
    for who in ("p1", "p2", "d1", "admin1", "org"):
        assert client.get(f"/files/{f['id']}", headers=auth(getattr(world, who))).status_code == 200, who
    for who in ("p3", "admin2"):
        assert client.get(f"/files/{f['id']}", headers=auth(getattr(world, who))).status_code == 404, who
    org_avatar = upload(client, s3, auth(world.org), "USER_AVATAR", None)
    assert client.get(f"/files/{org_avatar['id']}", headers=auth(world.org)).status_code == 200
    assert client.get(f"/files/{org_avatar['id']}", headers=auth(world.admin1)).status_code == 404


def test_deleted_files_are_served_to_org_only(client, db, s3, world, auth):
    f = upload(client, s3, auth(world.p1), "CHILD_PHOTO", world.c1, PNG, "image/png")
    assert client.delete(f"/files/{f['id']}", headers=auth(world.p1)).status_code == 204
    for who in ("p1", "d1", "admin1"):
        assert client.get(f"/files/{f['id']}", headers=auth(getattr(world, who))).status_code == 404, who
    r = client.get(f"/files/{f['id']}", headers=auth(world.org))
    assert r.status_code == 200 and r.json()["file"]["status"] == "DELETED"
    assert any(k[1] == db.get(File, uuid.UUID(f["id"])).key for k in s3.objects)


def test_unknown_files_and_anonymous_requests(client, world, auth):
    assert client.get(f"/files/{uuid.uuid4()}", headers=auth(world.org)).status_code == 404
    assert client.get(f"/files/{uuid.uuid4()}").status_code == 401


# --- deleting a file ----------------------------------------------------------------------

def test_uploader_can_discard_an_unattached_file(client, db, s3, world, auth):
    f = upload(client, s3, auth(world.p1), "MESSAGE_ATTACHMENT", world.c1)
    assert client.delete(f"/files/{f['id']}", headers=auth(world.p2)).status_code == 404
    assert client.delete(f"/files/{f['id']}", headers=auth(world.d1)).status_code == 404
    assert client.delete(f"/files/{f['id']}", headers=auth(world.p1)).status_code == 204
    assert client.delete(f"/files/{f['id']}", headers=auth(world.p1)).status_code == 404
    db.expire_all()
    assert db.get(File, uuid.UUID(f["id"])).status == RecordStatus.DELETED


def test_attached_files_cannot_be_deleted_directly(client, s3, world, auth):
    f = upload(client, s3, auth(world.p1), "CHILD_PHOTO", world.c1, PNG, "image/png")
    assert client.put(f"/children/{world.c1.id}/photo", headers=auth(world.p1), json={"file_id": f["id"]}).status_code == 200
    assert client.delete(f"/files/{f['id']}", headers=auth(world.p1)).status_code == 409
