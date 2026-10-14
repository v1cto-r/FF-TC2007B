"""The real thing: signed URLs against a running RustFS (or any S3). Skipped unless S3_INTEGRATION=1.

    S3_INTEGRATION=1 S3_HOST=localhost S3_PORT=9000 S3_PUBLIC_ENDPOINT=http://localhost:9000 \
    S3_ACCESS_KEY=... S3_SECRET_KEY=... pytest test/test_rustfs_integration.py
"""
import os
import time
import uuid

import httpx
import pytest

from app.config import settings
from app.storage import StorageService
from app.storage.policy import load_policy

pytestmark = pytest.mark.skipif(os.environ.get("S3_INTEGRATION") != "1", reason="set S3_INTEGRATION=1 to run against RustFS")

JPEG = b"\xff\xd8\xff\xe0" + b"j" * 2000
PDF = b"%PDF-1.7\n" + b"p" * 3000


@pytest.fixture
def real(client):
    storage = StorageService(settings, load_policy(settings))   # real boto3 clients, real endpoint
    for attempt in range(30):          # RustFS may still be starting when the tests begin
        try:
            storage.ensure_buckets()
            break
        except Exception:
            if attempt == 29:
                raise
            time.sleep(1)
    client.app.state.storage = storage
    return storage


def ask(client, headers, purpose, target, ct, size):
    r = client.post("/files", headers=headers, json=dict(id=str(uuid.uuid4()), purpose=purpose, target_id=str(target.id),
                                                         content_type=ct, size=size))
    assert r.status_code == 201, r.text
    return r.json()


def put(ticket, data, **override):
    up = ticket["upload"]
    headers = {**up["headers"], **override}
    headers.pop("Content-Length", None)      # httpx computes it from the body
    return httpx.put(up["url"], content=data, headers=headers)


def test_full_round_trip(client, real, world, auth):
    ticket = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, "image/jpeg", len(JPEG))
    fid = ticket["file"]["id"]
    assert client.post(f"/files/{fid}/complete", headers=auth(world.d1)).status_code == 409   # nothing there yet
    assert put(ticket, JPEG).status_code == 200
    done = client.post(f"/files/{fid}/complete", headers=auth(world.d1))
    assert done.status_code == 200 and done.json()["upload_state"] == "READY"

    dl = client.get(f"/files/{fid}", headers=auth(world.p1)).json()
    got = httpx.get(dl["url"])
    assert got.status_code == 200 and got.content == JPEG and got.headers["content-type"] == "image/jpeg"


def test_documents_go_to_their_own_bucket(client, real, world, auth):
    ticket = ask(client, auth(world.p1), "CHILD_DOCUMENT", world.c1, "application/pdf", len(PDF))
    assert ticket["upload"]["url"].split("?")[0].endswith(f"/files/child_document/{world.c1.id}/{ticket['file']['id']}.pdf")
    assert put(ticket, PDF).status_code == 200
    assert client.post(f"/files/{ticket['file']['id']}/complete", headers=auth(world.p1)).status_code == 200


def test_a_lying_client_is_caught_at_confirmation(client, real, world, auth):
    ticket = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, "image/jpeg", len(PDF))
    assert put(ticket, PDF).status_code == 200                      # S3 accepts the bytes: they have the declared size...
    r = client.post(f"/files/{ticket['file']['id']}/complete", headers=auth(world.d1))
    assert r.status_code == 422                                      # ...but they are a PDF pretending to be a JPEG
    assert client.get(f"/files/{ticket['file']['id']}", headers=auth(world.org)).status_code == 404


def test_the_signature_pins_the_content_type_and_size(client, real, world, auth):
    ticket = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, "image/jpeg", len(JPEG))
    assert put(ticket, JPEG, **{"Content-Type": "text/html"}).status_code == 403
    assert put(ticket, JPEG + b"more bytes than declared").status_code == 403


def test_a_signed_url_is_good_for_one_object_only(client, real, world, auth):
    ticket = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, "image/jpeg", len(JPEG))
    other_key = ticket["upload"]["url"].replace(ticket["file"]["id"], str(uuid.uuid4()))
    assert put({"upload": {**ticket["upload"], "url": other_key}}, JPEG).status_code == 403


def test_urls_expire(client, real, world, auth):
    real.policy.upload_expires = 1
    ticket = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, "image/jpeg", len(JPEG))
    time.sleep(2.5)
    assert put(ticket, JPEG).status_code == 403


def test_objects_are_not_public(client, real, world, auth):
    ticket = ask(client, auth(world.d1), "ACTIVITY_IMAGE", world.c1, "image/jpeg", len(JPEG))
    put(ticket, JPEG)
    bare = ticket["upload"]["url"].split("?")[0]
    assert httpx.get(bare).status_code in (403, 404)               # no signature, no access
    assert httpx.get(f"{settings.s3_public_endpoint}/images/").status_code in (403, 404)   # no listing either
