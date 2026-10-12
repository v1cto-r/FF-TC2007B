"""Writes an `audit_log` row for every create / update / delete of a Record.

Hooks the session's `before_flush`, so the audit rows go into the same transaction as the change:
either both are saved or neither is. The actor comes from `db.info["actor_id"]`, set by the auth
dependencies (a ContextVar would not survive FastAPI running dependencies and endpoints in
different threads). No actor means the system did it (startup seeding, cleanup jobs).

Only changes made through the ORM are seen. Never change a Record with a bulk `update()`.
"""
import uuid
from datetime import date, datetime, time
from decimal import Decimal
from enum import Enum

from sqlalchemy import event, inspect
from sqlalchemy.orm import Session
from sqlalchemy.orm.attributes import get_history

from app.database.models import AuditAction, AuditLog, Record, RecordStatus

SKIP_FIELDS = {"id", "created_at", "updated_at"}
REDACTED_FIELDS = {"password_hash"}


def _jsonable(value):
    if value is None or isinstance(value, (bool, int, float, str)):
        return value
    if isinstance(value, Enum):
        return value.value
    if isinstance(value, (uuid.UUID, Decimal)):
        return str(value)
    if isinstance(value, (datetime, date, time)):
        return value.isoformat()
    return str(value)


def _value(field: str, value):
    return "***" if field in REDACTED_FIELDS and value is not None else _jsonable(value)


def _snapshot(obj: Record) -> dict:
    mapper = inspect(obj).mapper
    return {a.key: _value(a.key, getattr(obj, a.key))
            for a in mapper.column_attrs if a.key not in SKIP_FIELDS and getattr(obj, a.key) is not None}


def _diff(obj: Record) -> dict:
    changes = {}
    for attr in inspect(obj).mapper.column_attrs:
        if attr.key in SKIP_FIELDS:
            continue
        hist = get_history(obj, attr.key)
        if hist.has_changes():
            old = hist.deleted[0] if hist.deleted else None
            new = hist.added[0] if hist.added else None
            changes[attr.key] = [_value(attr.key, old), _value(attr.key, new)]
    return changes


def _action_for(changes: dict) -> AuditAction:
    if "status" in changes:
        new = changes["status"][1]
        if new == RecordStatus.DELETED.value:
            return AuditAction.DELETE
        if new == RecordStatus.ARCHIVED.value:
            return AuditAction.ARCHIVE
        return AuditAction.UNARCHIVE
    return AuditAction.UPDATE


@event.listens_for(Session, "before_flush")
def _audit_before_flush(session: Session, flush_context, instances) -> None:
    actor = session.info.get("actor_id")
    entries = []
    for obj in session.new:
        if isinstance(obj, Record):
            if obj.id is None:  # the column default runs later, at INSERT; the log needs it now
                obj.id = uuid.uuid4()
            entries.append(AuditLog(actor_id=actor, action=AuditAction.CREATE, resource_type=obj.__tablename__,
                                    resource_id=obj.id, changes=_snapshot(obj)))
    for obj in session.dirty:
        if isinstance(obj, Record) and session.is_modified(obj, include_collections=False):
            changes = _diff(obj)
            if changes:
                entries.append(AuditLog(actor_id=actor, action=_action_for(changes),
                                        resource_type=obj.__tablename__, resource_id=obj.id, changes=changes))
    session.add_all(entries)
