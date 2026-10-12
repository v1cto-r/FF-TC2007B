from pathlib import Path

import yaml
from pydantic import BaseModel, model_validator

from app.config import Settings
from app.database.models import FilePurpose

from .errors import InvalidUpload


class KindPolicy(BaseModel):
    content_types: list[str]
    max_bytes: int


class PurposePolicy(BaseModel):
    bucket: str
    kinds: list[str]


class Limit(BaseModel):
    per_minute: int | None = None
    per_day: int | None = None


class RateLimits(BaseModel):
    upload: Limit
    download: Limit


class Policy(BaseModel):
    kinds: dict[str, KindPolicy]
    buckets: dict[str, list[str]]  # bucket -> allowed kinds
    purposes: dict[str, PurposePolicy]
    rate_limits: RateLimits
    upload_expires: int
    download_expires: int

    @model_validator(mode="after")
    def _check_refs(self):
        for bucket, kinds in self.buckets.items():
            for k in kinds:
                if k not in self.kinds:
                    raise ValueError(f"bucket {bucket!r} references unknown kind {k!r}")
        missing = {p.value for p in FilePurpose} - set(self.purposes)
        if missing:
            raise ValueError(f"purposes without a policy: {sorted(missing)}")
        for name, p in self.purposes.items():
            if name not in FilePurpose.__members__:
                raise ValueError(f"unknown purpose {name!r}")
            if p.bucket not in self.buckets:
                raise ValueError(f"purpose {name}: unknown bucket {p.bucket!r}")
            for k in p.kinds:
                if k not in self.buckets[p.bucket]:
                    raise ValueError(f"purpose {name}: kind {k!r} not allowed in bucket {p.bucket!r}")
        return self

    def resolve(self, purpose: FilePurpose, content_type: str, size: int) -> tuple[str, str]:
        """(bucket, kind) for an upload, or InvalidUpload if the type or size isn't acceptable."""
        p = self.purposes[purpose.value]
        for kind in p.kinds:
            k = self.kinds[kind]
            if content_type in k.content_types:
                if size <= 0 or size > k.max_bytes:
                    raise InvalidUpload(f"Size must be between 1 and {k.max_bytes} bytes")
                return p.bucket, kind
        raise InvalidUpload(f"Content type not allowed here: {content_type}")


def load_policy(settings: Settings, path: str | None = None) -> Policy:
    raw = yaml.safe_load(Path(path or settings.storage_policy_path).read_text())
    default_max = {"image": settings.storage_max_image_bytes, "file": settings.storage_max_file_bytes}
    kinds = {
        name: KindPolicy(
            content_types=k["content_types"],
            max_bytes=k.get("max_bytes", default_max.get(name, settings.storage_max_file_bytes)),
        )
        for name, k in raw["kinds"].items()
    }
    rl = raw.get("rate_limits", {})
    rate_limits = RateLimits(
        upload=Limit(**{"per_minute": settings.storage_upload_per_minute,
                        "per_day": settings.storage_upload_per_day, **rl.get("upload", {})}),
        download=Limit(**{"per_minute": settings.storage_download_per_minute, **rl.get("download", {})}),
    )
    return Policy(
        kinds=kinds,
        buckets={b: v["kinds"] for b, v in raw["buckets"].items()},
        purposes={n: PurposePolicy(**p) for n, p in raw["purposes"].items()},
        rate_limits=rate_limits,
        upload_expires=raw.get("upload_expires", settings.storage_upload_expires),
        download_expires=raw.get("download_expires", settings.storage_download_expires),
    )
