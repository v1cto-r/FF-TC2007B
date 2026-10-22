"""The S3 side only: presigned URLs, and looking at an uploaded object. No authorization here."""
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any

import boto3
from botocore.config import Config
from botocore.exceptions import ClientError

from app.config import Settings

from .magic import HEAD_BYTES
from .policy import Policy


@dataclass(frozen=True)
class SignedUrl:
    url: str
    headers: dict[str, str]
    expires_at: datetime


@dataclass(frozen=True)
class ObjectInfo:
    size: int
    content_type: str


class StorageService:
    def __init__(self, settings: Settings, policy: Policy, s3_client: Any = None, presign_client: Any = None):
        self.policy = policy
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
        for bucket in self.policy.buckets:
            try:
                self._s3.head_bucket(Bucket=bucket)
            except ClientError:
                self._s3.create_bucket(Bucket=bucket)

    def presign_put(self, bucket: str, key: str, content_type: str, size: int) -> SignedUrl:
        expires = self.policy.upload_expires
        url = self._presign.generate_presigned_url(
            "put_object",
            Params={"Bucket": bucket, "Key": key, "ContentType": content_type, "ContentLength": size},
            ExpiresIn=expires,
        )
        return SignedUrl(url, {"Content-Type": content_type, "Content-Length": str(size)},
                         datetime.now(timezone.utc) + timedelta(seconds=expires))

    def presign_get(self, bucket: str, key: str) -> SignedUrl:
        expires = self.policy.download_expires
        url = self._presign.generate_presigned_url(
            "get_object", Params={"Bucket": bucket, "Key": key}, ExpiresIn=expires)
        return SignedUrl(url, {}, datetime.now(timezone.utc) + timedelta(seconds=expires))

    def head(self, bucket: str, key: str) -> ObjectInfo | None:
        """Size and content type of the stored object, or None if it was never uploaded."""
        try:
            r = self._s3.head_object(Bucket=bucket, Key=key)
        except ClientError as e:
            if e.response.get("Error", {}).get("Code") in ("404", "NoSuchKey", "NotFound"):
                return None
            raise
        return ObjectInfo(size=r["ContentLength"], content_type=r.get("ContentType", ""))

    def read_head(self, bucket: str, key: str) -> bytes:
        r = self._s3.get_object(Bucket=bucket, Key=key, Range=f"bytes=0-{HEAD_BYTES - 1}")
        return r["Body"].read()
