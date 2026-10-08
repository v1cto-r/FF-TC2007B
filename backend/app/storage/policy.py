from pathlib import Path
from typing import Literal

import yaml
from pydantic import BaseModel, model_validator

from app.config import Settings


class KindPolicy(BaseModel):
    content_types: list[str]
    max_bytes: int


SCOPES = ("estancia", "salon", "child")


class UploadRule(BaseModel):
    bucket: str
    kind: str
    scope: Literal["estancia", "salon", "child"]  # what the caller must be tied to
    object_name: str | None = None  # fixed file name (overwrites); random if omitted


class Limit(BaseModel):
    per_minute: int | None = None
    per_day: int | None = None


class RateLimits(BaseModel):
    upload: Limit
    download: Limit


class RolePolicy(BaseModel):
    upload: list[UploadRule] = []
    download: dict[str, list[Literal["estancia", "salon", "child", "any"]]] = {}  # bucket -> scopes
    rate_limits: RateLimits | None = None


class Policy(BaseModel):
    kinds: dict[str, KindPolicy]
    buckets: dict[str, list[str]]  # bucket -> allowed kinds
    roles: dict[str, RolePolicy]
    rate_limits: RateLimits
    upload_expires: int
    download_expires: int

    @model_validator(mode="after")
    def _check_refs(self):
        for bucket, kinds in self.buckets.items():
            for k in kinds:
                if k not in self.kinds:
                    raise ValueError(f"bucket {bucket!r} references unknown kind {k!r}")
        for role, rp in self.roles.items():
            for rule in rp.upload:
                if rule.bucket not in self.buckets:
                    raise ValueError(f"role {role}: unknown bucket {rule.bucket!r}")
                if rule.kind not in self.buckets[rule.bucket]:
                    raise ValueError(f"role {role}: kind {rule.kind!r} not allowed in {rule.bucket!r}")
            for b in rp.download:  # keys of the bucket -> scopes map
                if b not in self.buckets:
                    raise ValueError(f"role {role}: unknown bucket {b!r}")
        return self

    def limits_for(self, role: str) -> RateLimits:
        rp = self.roles.get(role)
        return (rp.rate_limits if rp and rp.rate_limits else None) or self.rate_limits


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
    roles = {}
    for role, rp in raw["roles"].items():
        rp = dict(rp)
        if "rate_limits" in rp:
            base = rate_limits.model_dump()
            for action, vals in rp["rate_limits"].items():
                base[action].update(vals)
            rp["rate_limits"] = base
        roles[role] = RolePolicy(**rp)
    return Policy(
        kinds=kinds,
        buckets={b: v["kinds"] for b, v in raw["buckets"].items()},
        roles=roles,
        rate_limits=rate_limits,
        upload_expires=raw.get("upload_expires", settings.storage_upload_expires),
        download_expires=raw.get("download_expires", settings.storage_download_expires),
    )
