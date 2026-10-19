"""One JSON line on stdout for every write. Reads are not logged.

* `record_write`: a create / update / delete / archive / unarchive of a Record, written by the audit hook when the
  session flushes. Names the fields that changed, never their values (the values are in `audit_log`).
* `http_write`: every POST / PUT / PATCH / DELETE, whatever its outcome (see middleware.py).

"""
import json
import logging
import sys
from contextvars import ContextVar
from datetime import datetime, timezone

from app.observability.metrics import RECORD_WRITES

request_id: ContextVar[str | None] = ContextVar("request_id", default=None)


class _JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        ts = datetime.fromtimestamp(record.created, timezone.utc).isoformat(timespec="milliseconds")
        return json.dumps({"ts": ts, **record.msg}, default=str)


log = logging.getLogger("ff.writes")
if not log.handlers:
    _handler = logging.StreamHandler(sys.stdout)
    _handler.setFormatter(_JsonFormatter())
    log.addHandler(_handler)
    log.setLevel(logging.INFO)
    log.propagate = False


def record_write(entry) -> None:
    """Called by the audit hook for each AuditLog row it writes."""
    RECORD_WRITES.labels(entry.resource_type, entry.action.value).inc()
    log.info({"event": "record_write", "request_id": request_id.get(), "action": entry.action.value,
              "resource": entry.resource_type, "resource_id": str(entry.resource_id),
              "actor_id": str(entry.actor_id) if entry.actor_id else None, "fields": sorted(entry.changes)})
