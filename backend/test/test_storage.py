import os

for k, v in {"POSTGRES_USER": "u", "POSTGRES_PASSWORD": "p", "POSTGRES_DB": "d",
             "S3_ACCESS_KEY": "ak", "S3_SECRET_KEY": "sk", "SECRET_KEY": "s"}.items():
    os.environ.setdefault(k, v)

import pytest

from app.config import Settings
from app.storage import CurrentUser, InvalidUpload, RateLimitExceeded, StorageForbidden, StorageService
from app.storage.policy import load_policy
from app.storage.rate_limit import InMemoryRateLimiter

PDF = "application/pdf"
settings = Settings()
policy = load_policy(settings, "app/storage/policy.yml")


class Clock:
    t = 1000.0

    def __call__(self):
        return self.t


@pytest.fixture
def clock():
    return Clock()


@pytest.fixture
def svc(clock):
    return StorageService(settings, policy, InMemoryRateLimiter(clock))


def U(id, role, **sc):
    return CurrentUser(id, role, {k: frozenset(v) for k, v in sc.items()})


padre = U("p1", "PADRE", child=["c1"])
docente = U("d1", "DOCENTE", salon=["s1"], child=["c1"])
admin = U("a1", "ADMIN", estancia=["e1"])
org = U("o1", "ORG")


def up(svc, user, bucket, kind, scope_id, ct="image/png", size=100):
    return svc.create_upload(user, bucket, kind, scope_id, ct, size)


def test_padre_image_and_file(svc):
    assert up(svc, padre, "images", "image", "c1").key.startswith("child/c1/")
    assert up(svc, padre, "files", "file", "c1", PDF).key.endswith(".pdf")


def test_padre_only_own_child(svc):
    with pytest.raises(StorageForbidden):
        up(svc, padre, "images", "image", "c2")


def test_docente_images_only_in_own_salon(svc):
    up(svc, docente, "images", "image", "s1", "image/jpeg")
    with pytest.raises(StorageForbidden):
        up(svc, docente, "images", "image", "s2")
    with pytest.raises(StorageForbidden):
        up(svc, docente, "files", "file", "s1", PDF)


def test_admin_only_own_logo(svc):
    assert up(svc, admin, "estancias", "image", "e1", "image/webp").key == "estancia/e1/logo.webp"
    with pytest.raises(StorageForbidden):
        up(svc, admin, "estancias", "image", "e2")
    with pytest.raises(StorageForbidden):
        up(svc, admin, "images", "image", "e1")


def test_org_cannot_upload_but_can_download(svc):
    with pytest.raises(StorageForbidden):
        up(svc, org, "estancias", "image", "e1")
    assert "estancias" in svc.create_download(org, "estancias", "estancia/e1/logo.png").url


def test_download_scope(svc):
    assert svc.create_download(padre, "images", "child/c1/x.png")
    with pytest.raises(StorageForbidden):
        svc.create_download(padre, "images", "child/c2/x.png")
    with pytest.raises(StorageForbidden):
        svc.create_download(padre, "images", "salon/s1/x.png")


def test_type_and_size(svc):
    with pytest.raises(InvalidUpload):
        up(svc, padre, "images", "image", "c1", "image/gif")
    with pytest.raises(InvalidUpload):
        up(svc, padre, "images", "image", "c1", size=5 * 1024 * 1024 + 1)
    with pytest.raises(InvalidUpload):
        up(svc, padre, "files", "file", "c1", PDF, 10 * 1024 * 1024 + 1)


def test_signed_headers_in_url(svc):
    url = up(svc, padre, "images", "image", "c1", size=123).url
    assert "content-length" in url.lower() and "content-type" in url.lower()
    assert url.startswith(settings.s3_public_endpoint)


def test_download_key_traversal(svc):
    with pytest.raises(InvalidUpload):
        svc.create_download(padre, "images", "child/../x")
    with pytest.raises(StorageForbidden):
        svc.create_download(padre, "estancias", "estancia/e1/logo.png")


def test_rate_limit_minute_and_reset(svc, clock):
    for _ in range(10):
        up(svc, padre, "images", "image", "c1", size=1)
    with pytest.raises(RateLimitExceeded) as e:
        up(svc, padre, "images", "image", "c1", size=1)
    assert e.value.retry_after >= 1
    clock.t += 61
    up(svc, padre, "images", "image", "c1", size=1)


def test_rate_limit_daily(svc, clock):
    for i in range(200):
        clock.t += 7  # stays under 10/min
        up(svc, padre, "images", "image", "c1", size=1)
    clock.t += 7
    with pytest.raises(RateLimitExceeded):
        up(svc, padre, "images", "image", "c1", size=1)


def test_denied_requests_dont_burn_quota(svc):
    for _ in range(20):
        with pytest.raises(StorageForbidden):
            up(svc, org, "images", "image", "c1", size=1)


def test_unknown_bucket_fails_policy(tmp_path):
    bad = tmp_path / "p.yml"
    bad.write_text("kinds: {image: {content_types: [image/png]}}\nbuckets: {a: {kinds: [image]}}\n"
                   "roles: {PADRE: {upload: [{bucket: nope, kind: image, scope: child}]}}\n")
    with pytest.raises(ValueError):
        load_policy(settings, str(bad))
