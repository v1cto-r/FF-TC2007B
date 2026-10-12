"""Soft delete, archive and visibility rules shared by every router.

* status NULL      -> active
* ARCHIVED         -> read-only, still listed
* DELETED          -> hidden from everyone except ORG, never removed from the database

Deleting a row that other live rows depend on is refused (409). Rows that are *part of* the
deleted row (a daily log's check-in, stay, check-out and activities) are deleted with it.
"""
from collections.abc import Iterable

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.database.models import Record, RecordStatus


def live(model):
    """SQL condition: the row is not DELETED (active or ARCHIVED)."""
    return model.status.is_distinct_from(RecordStatus.DELETED)


def active(model):
    """SQL condition: the row is active (neither ARCHIVED nor DELETED)."""
    return model.status.is_(None)


def apply_visibility(stmt, model, access: Access, include_deleted: bool = False):
    """Hide DELETED rows; ORG may ask for them with ?include_deleted=true."""
    if include_deleted:
        if not access.is_org:
            raise HTTPException(403, "Only ORG can list deleted records")
        return stmt
    return stmt.where(live(model))


def get_or_404(db: Session, model, record_id, access: Access | None = None):
    obj = db.get(model, record_id)
    if obj is None or (obj.status == RecordStatus.DELETED and not (access and access.is_org)):
        raise HTTPException(404, f"{model.__name__} not found")
    return obj


def ensure_writable(obj: Record) -> None:
    """ARCHIVED and DELETED rows can't be edited, and nothing new can hang from them."""
    if obj.status is not None:
        raise HTTPException(409, f"{type(obj).__name__} is {obj.status.value.lower()} and read-only")


def soft_delete(db: Session, obj: Record, *, blockers: Iterable[tuple] = (), parts: Iterable[tuple] = ()) -> None:
    """Mark `obj` DELETED. Idempotent.

    blockers: (Model, fk_column) pairs; any *active* row pointing at obj makes this a 409.
    parts:    (Model, fk_column) pairs; rows pointing at obj are deleted along with it.
    """
    if obj.status == RecordStatus.DELETED:
        return
    db.flush()  # sessions don't autoflush: make earlier changes of this request visible to the checks below
    for model, column in blockers:
        if db.scalar(select(model.id).where(column == obj.id, model.status.is_(None)).limit(1)):
            raise HTTPException(409, f"Can't delete: it still has active {model.__tablename__}")
    for model, column in parts:
        for part in db.scalars(select(model).where(column == obj.id, live(model))):
            part.status = RecordStatus.DELETED
    obj.status = RecordStatus.DELETED


def archive(obj: Record) -> None:
    if obj.status == RecordStatus.DELETED:
        raise HTTPException(409, f"{type(obj).__name__} is deleted")
    obj.status = RecordStatus.ARCHIVED


def unarchive(obj: Record) -> None:
    if obj.status != RecordStatus.ARCHIVED:
        raise HTTPException(409, f"{type(obj).__name__} is not archived")
    obj.status = None
