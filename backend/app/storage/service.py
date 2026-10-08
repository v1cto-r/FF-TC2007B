import mimetypes
import uuid
from collections.abc import Mapping
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import Any

import boto3
from botocore.config import Config
from botocore.exceptions import ClientError

from app.config import Settings

from .errors import InvalidUpload, StorageForbidden
from .policy import Policy
from .rate_limit import RateLimiter

_EXT = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
    "application/pdf": "pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "docx",
}


@dataclass(frozen=True)
class CurrentUser:
    id: str
    role: str
    # scope type -> ids the user may act on, e.g. {"child": {...}, "salon": {...}, "estancia": {...}}
    scopes: Mapping[str, frozenset[str]] = field(default_factory=dict)

    def can(self, scope: str, scope_id: str) -> bool:
        return scope_id in self.scopes.get(scope, ())


@dataclass(frozen=True)
class PresignedUpload:
    url: str
    key: str
    bucket: str
    headers: dict[str, str]
    expires_at: datetime


@dataclass(frozen=True)
class PresignedDownload:
    url: str
    expires_at: datetime


class StorageService:
    def __init__(self, settings: Settings, policy: Policy, rate_limiter: RateLimiter,
                 s3_client: Any = None, presign_client: Any = None):
        self._policy = policy
        self._limiter = rate_limiter
        cfg = Config(signature_version="s3v4", s3={"addressing_style": "path"})

        def make_client(endpoint: str):
            return boto3.client(
                "s3", endpoint_url=endpoint, region_name=settings.s3_region,
                aws_access_key_id=settings.s3_access_key,
                aws_secret_access_key=settings.s3_secret_key, config=cfg,
            )

        # Internal client talks to RustFS; presign client signs for the host the user reaches.
        self._s3 = s3_client or make_client(settings.s3_endpoint)
        self._presign = presign_client or make_client(settings.s3_public_endpoint)

    def ensure_buckets(self) -> None:
        for bucket in self._policy.buckets:
            try:
                self._s3.head_bucket(Bucket=bucket)
            except ClientError:
                self._s3.create_bucket(Bucket=bucket)

    def create_upload(self, user: CurrentUser, bucket: str, kind: str, scope_id: str,
                      content_type: str, size: int) -> PresignedUpload:
        role = self._policy.roles.get(user.role)
        rule = next((r for r in (role.upload if role else [])
                     if r.bucket == bucket and r.kind == kind), None)
        if rule is None:
            raise StorageForbidden("Role cannot upload this kind to this bucket")

        kind_policy = self._policy.kinds[kind]
        if content_type not in kind_policy.content_types:
            raise InvalidUpload(f"Content type not allowed: {content_type}")
        if size <= 0 or size > kind_policy.max_bytes:
            raise InvalidUpload(f"Size must be between 1 and {kind_policy.max_bytes} bytes")

        ext = _EXT.get(content_type) or (mimetypes.guess_extension(content_type) or "").lstrip(".")
        if not user.can(rule.scope, scope_id):
            raise StorageForbidden(f"Not allowed on this {rule.scope}")
        name = rule.object_name or uuid.uuid4().hex
        key = f"{rule.scope}/{scope_id}/{name}.{ext}" if ext else f"{rule.scope}/{scope_id}/{name}"

        # Counted only after authz/validation, so denied requests don't burn quota
        self._limiter.check(user.id, "upload", self._policy.limits_for(user.role).upload)

        expires = self._policy.upload_expires
        url = self._presign.generate_presigned_url(
            "put_object",
            Params={"Bucket": bucket, "Key": key, "ContentType": content_type, "ContentLength": size},
            ExpiresIn=expires,
        )
        return PresignedUpload(
            url=url, key=key, bucket=bucket,
            headers={"Content-Type": content_type, "Content-Length": str(size)},
            expires_at=datetime.now(timezone.utc) + timedelta(seconds=expires),
        )

    def create_download(self, user: CurrentUser, bucket: str, key: str) -> PresignedDownload:
        role = self._policy.roles.get(user.role)
        if role is None or bucket not in role.download:
            raise StorageForbidden("Role cannot read this bucket")
        parts = key.split("/")
        if len(parts) != 3 or not all(parts) or ".." in parts:
            raise InvalidUpload("Invalid key")
        scopes = role.download[bucket]
        if "any" not in scopes and not (parts[0] in scopes and user.can(parts[0], parts[1])):
            raise StorageForbidden("Not allowed to read this object")

        self._limiter.check(user.id, "download", self._policy.limits_for(user.role).download)

        expires = self._policy.download_expires
        url = self._presign.generate_presigned_url(
            "get_object", Params={"Bucket": bucket, "Key": key}, ExpiresIn=expires,
        )
        return PresignedDownload(url=url, expires_at=datetime.now(timezone.utc) + timedelta(seconds=expires))
