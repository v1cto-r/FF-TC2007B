"""The storage building blocks: policy file, magic bytes, presigned URLs, the S3 client wrapper."""
from pathlib import Path
from urllib.parse import parse_qs, urlparse

import pytest
import yaml

from app.config import settings
from app.database.models import FilePurpose
from app.storage import InvalidUpload, StorageService
from app.storage.magic import DOCX, EXTENSIONS, HEAD_BYTES, matches
from app.storage.policy import Policy, load_policy

POLICY_FILE = Path(__file__).resolve().parent.parent / "app/storage/policy.yml"


def write_policy(tmp_path, mutate=lambda raw: None):
    raw = yaml.safe_load(POLICY_FILE.read_text())
    mutate(raw)
    p = tmp_path / "policy.yml"
    p.write_text(yaml.safe_dump(raw))
    return str(p)


# --- policy -------------------------------------------------------------------------------

def test_shipped_policy_loads_and_covers_every_purpose():
    policy = load_policy(settings)
    assert set(policy.purposes) == {p.value for p in FilePurpose}
    assert set(policy.buckets) == {"estancias", "images", "files"}


@pytest.mark.parametrize("purpose,ct,ok", [
    (FilePurpose.ACTIVITY_IMAGE, "image/jpeg", True), (FilePurpose.ACTIVITY_IMAGE, "image/png", True),
    (FilePurpose.ACTIVITY_IMAGE, "image/webp", True), (FilePurpose.ACTIVITY_IMAGE, "application/pdf", False),
    (FilePurpose.ACTIVITY_IMAGE, "image/svg+xml", False), (FilePurpose.ACTIVITY_IMAGE, "image/gif", False),
    (FilePurpose.CHILD_DOCUMENT, "application/pdf", True), (FilePurpose.CHILD_DOCUMENT, DOCX, True),
    (FilePurpose.CHILD_DOCUMENT, "image/jpeg", True), (FilePurpose.CHILD_DOCUMENT, "application/zip", False),
    (FilePurpose.CHILD_DOCUMENT, "application/x-msdownload", False),
    (FilePurpose.ESTANCIA_LOGO, "application/pdf", False), (FilePurpose.USER_AVATAR, DOCX, False),
])
def test_content_types_per_purpose(purpose, ct, ok):
    policy = load_policy(settings)
    if ok:
        policy.resolve(purpose, ct, 10)
    else:
        with pytest.raises(InvalidUpload):
            policy.resolve(purpose, ct, 10)


def test_buckets_and_kinds_are_resolved():
    policy = load_policy(settings)
    assert policy.resolve(FilePurpose.ACTIVITY_IMAGE, "image/jpeg", 10) == ("images", "image")
    assert policy.resolve(FilePurpose.ESTANCIA_LOGO, "image/png", 10) == ("estancias", "image")
    assert policy.resolve(FilePurpose.CHILD_DOCUMENT, "application/pdf", 10) == ("files", "file")
    assert policy.resolve(FilePurpose.CHILD_DOCUMENT, "image/png", 10) == ("files", "image")   # a scan


def test_size_limits_follow_the_kind_of_file():
    policy = load_policy(settings)
    mb = 1024 * 1024
    policy.resolve(FilePurpose.CHILD_DOCUMENT, "application/pdf", 10 * mb)
    policy.resolve(FilePurpose.CHILD_DOCUMENT, "image/jpeg", 5 * mb)
    for purpose, ct, size in ((FilePurpose.CHILD_DOCUMENT, "application/pdf", 10 * mb + 1),
                              (FilePurpose.CHILD_DOCUMENT, "image/jpeg", 5 * mb + 1),
                              (FilePurpose.USER_AVATAR, "image/jpeg", 0), (FilePurpose.USER_AVATAR, "image/jpeg", -1)):
        with pytest.raises(InvalidUpload):
            policy.resolve(purpose, ct, size)


def test_env_settings_set_default_limits(tmp_path, monkeypatch):
    def strip_limits(raw):
        for kind in raw["kinds"].values():
            kind.pop("max_bytes")
        raw.pop("rate_limits")

    path = write_policy(tmp_path, strip_limits)
    monkeypatch.setattr(settings, "storage_max_image_bytes", 1000)
    monkeypatch.setattr(settings, "storage_max_file_bytes", 2000)
    monkeypatch.setattr(settings, "storage_upload_per_minute", 7)
    policy = load_policy(settings, path)
    assert policy.kinds["image"].max_bytes == 1000 and policy.kinds["file"].max_bytes == 2000
    assert policy.rate_limits.upload.per_minute == 7 and policy.rate_limits.download.per_minute == 300


def test_yaml_overrides_the_env_defaults(tmp_path):
    path = write_policy(tmp_path, lambda raw: raw["rate_limits"].update(upload={"per_minute": 99, "per_day": 5}))
    limits = load_policy(settings, path).rate_limits.upload
    assert (limits.per_minute, limits.per_day) == (99, 5)


@pytest.mark.parametrize("mutate,message", [
    (lambda r: r["buckets"].update(images={"kinds": ["video"]}), "unknown kind"),
    (lambda r: r["purposes"].pop("USER_AVATAR"), "without a policy"),
    (lambda r: r["purposes"].update(HACK={"bucket": "images", "kinds": ["image"]}), "unknown purpose"),
    (lambda r: r["purposes"]["USER_AVATAR"].update(bucket="nowhere"), "unknown bucket"),
    (lambda r: r["purposes"]["USER_AVATAR"].update(kinds=["file"]), "not allowed in bucket"),
])
def test_broken_policies_are_rejected_at_load(tmp_path, mutate, message):
    with pytest.raises(ValueError, match=message):
        load_policy(settings, write_policy(tmp_path, mutate))


# --- magic bytes --------------------------------------------------------------------------

REAL = {
    "image/jpeg": b"\xff\xd8\xff\xe0\x00\x10JFIF",
    "image/png": b"\x89PNG\r\n\x1a\n\x00\x00",
    "image/webp": b"RIFF\x24\x00\x00\x00WEBPVP8 ",
    "application/pdf": b"%PDF-1.4\n%\xe2\xe3",
    DOCX: b"PK\x03\x04\x14\x00\x06\x00",
}


@pytest.mark.parametrize("ct", REAL)
def test_real_headers_match_their_type_and_no_other(ct):
    assert matches(ct, REAL[ct])
    for other, head in REAL.items():
        if other != ct:
            assert not matches(ct, head), f"{other} bytes accepted as {ct}"


@pytest.mark.parametrize("head", [b"", b"\x00" * 16, b"<html><script>", b"MZ\x90\x00", b"GIF89a"])
def test_garbage_matches_nothing(head):
    assert not any(matches(ct, head) for ct in REAL)


def test_unknown_type_never_matches_and_every_accepted_type_has_an_extension():
    assert not matches("application/octet-stream", b"anything")
    accepted = {ct for k in load_policy(settings).kinds.values() for ct in k.content_types}
    assert accepted <= set(EXTENSIONS) and accepted <= set(REAL)


# --- the S3 wrapper -----------------------------------------------------------------------

@pytest.fixture
def storage(s3):
    return StorageService(settings, load_policy(settings), s3_client=s3)


def test_ensure_buckets_creates_the_missing_ones_only(storage, s3):
    s3.buckets.add("images")
    storage.ensure_buckets()
    assert s3.buckets == {"estancias", "images", "files"}
    storage.ensure_buckets()  # again: nothing breaks


def test_upload_url_is_signed_for_the_public_host_with_the_exact_headers(storage):
    signed = storage.presign_put("images", "a/b.jpg", "image/jpeg", 123)
    u = urlparse(signed.url)
    assert f"{u.scheme}://{u.netloc}" == settings.s3_public_endpoint and u.path == "/images/a/b.jpg"
    q = parse_qs(u.query)
    assert q["X-Amz-Expires"] == ["300"] and q["X-Amz-Algorithm"] == ["AWS4-HMAC-SHA256"]
    assert signed.headers == {"Content-Type": "image/jpeg", "Content-Length": "123"}
    assert "sk" not in signed.url          # the secret key is never in the URL


def test_download_url_expires_in_15_minutes(storage):
    signed = storage.presign_get("files", "x/y.pdf")
    assert parse_qs(urlparse(signed.url).query)["X-Amz-Expires"] == ["900"] and signed.headers == {}


def test_urls_depend_on_the_object(storage):
    a, b = storage.presign_get("images", "a.jpg"), storage.presign_get("images", "b.jpg")
    sig = lambda s: parse_qs(urlparse(s.url).query)["X-Amz-Signature"]
    assert sig(a) != sig(b)


def test_head_and_read_head(storage, s3):
    assert storage.head("images", "nope") is None
    s3.put("images", "k.png", b"\x89PNG\r\n\x1a\n" + b"x" * 100, "image/png")
    info = storage.head("images", "k.png")
    assert (info.size, info.content_type) == (108, "image/png")
    assert storage.read_head("images", "k.png") == (b"\x89PNG\r\n\x1a\n" + b"x" * 100)[:HEAD_BYTES]


def test_other_storage_errors_are_not_swallowed(storage, s3, monkeypatch):
    from botocore.exceptions import ClientError
    monkeypatch.setattr(s3, "head_object", lambda **kw: (_ for _ in ()).throw(ClientError({"Error": {"Code": "500"}}, "HeadObject")))
    with pytest.raises(ClientError):
        storage.head("images", "k")
