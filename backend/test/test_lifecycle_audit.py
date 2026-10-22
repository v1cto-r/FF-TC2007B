"""Nothing is hard-deleted, every change is audited, and DELETED rows stop blocking unique values."""
import uuid

import pytest
from sqlalchemy import select, text
from sqlalchemy.exc import DBAPIError, IntegrityError

from app.database import models as m
from app.database.lifecycle import archive, ensure_writable, soft_delete, unarchive
from app.database.models import AuditAction, AuditLog, RecordStatus
from fastapi import HTTPException

SOFT_TABLES = ["users", "estancias", "salones", "children", "daily_logs", "check_ins", "stays", "check_outs",
               "activity_records", "activity_images", "notices", "messages", "files", "child_documents"]


def audit_for(db, obj):
    return list(db.scalars(select(AuditLog).where(AuditLog.resource_id == obj.id).order_by(AuditLog.created_at)))


# --- the database refuses to forget -------------------------------------------------------

@pytest.mark.parametrize("table", SOFT_TABLES)
def test_truncate_is_rejected_on_every_soft_table(db, world, table):
    # a DELETE only fires the row trigger for rows that exist, and these tables are empty except a few,
    # so use the statement-level TRUNCATE guard, which fires regardless
    with pytest.raises(DBAPIError, match="Hard delete is not allowed"):
        db.execute(text(f"TRUNCATE public.{table} CASCADE"))
    db.rollback()


def test_deleting_a_row_is_rejected(db, world):
    with pytest.raises(DBAPIError, match="Hard delete is not allowed on public.children"):
        db.execute(text("DELETE FROM public.children WHERE id = :i"), {"i": world.c1.id})
    db.rollback()
    assert db.get(m.Child, world.c1.id) is not None


def test_orm_delete_is_rejected(db, world):
    db.delete(world.c4)
    with pytest.raises(DBAPIError, match="Hard delete is not allowed"):
        db.flush()
    db.rollback()


def test_audit_log_is_append_only(db, world):
    entry = db.scalars(select(AuditLog)).first()
    for sql in ("UPDATE public.audit_log SET resource_type = 'x'", "DELETE FROM public.audit_log",
                "TRUNCATE public.audit_log"):
        with pytest.raises(DBAPIError, match="append-only"):
            db.execute(text(sql))
        db.rollback()
    assert db.get(AuditLog, entry.id).resource_type == entry.resource_type


# --- the audit listener -------------------------------------------------------------------

def test_create_is_audited_with_a_snapshot(db, world):
    log = audit_for(db, world.c1)
    assert [e.action for e in log] == [AuditAction.CREATE]
    assert log[0].resource_type == "children"
    assert log[0].actor_id is None  # made by the test setup, not by a signed-in user
    assert log[0].changes["full_name"] == "c1" and log[0].changes["parent_id"] == str(world.p1.id)
    assert "updated_at" not in log[0].changes and "id" not in log[0].changes


def test_update_is_audited_as_old_and_new_values(db, world):
    world.c1.full_name = "Renamed"
    db.info["actor_id"] = world.admin1.id
    db.commit()
    entry = audit_for(db, world.c1)[-1]
    assert entry.action == AuditAction.UPDATE and entry.actor_id == world.admin1.id
    assert entry.changes == {"full_name": ["c1", "Renamed"]}


def test_unchanged_save_writes_no_audit_row(db, world):
    before = len(audit_for(db, world.c1))
    world.c1.full_name = "c1"  # same value
    db.commit()
    assert len(audit_for(db, world.c1)) == before


def test_status_changes_map_to_actions(db, world):
    c = world.c3
    for new, action in ((RecordStatus.ARCHIVED, AuditAction.ARCHIVE), (None, AuditAction.UNARCHIVE),
                        (RecordStatus.DELETED, AuditAction.DELETE)):
        c.status = new
        db.commit()
        last = audit_for(db, c)[-1]
        assert last.action == action
        assert last.changes["status"][1] == (new.value if new else None)


def test_password_hash_is_never_written_to_the_audit_log(db, world):
    world.p1.password_hash = "a-new-secret-hash"
    db.commit()
    entry = audit_for(db, world.p1)[-1]
    assert entry.changes["password_hash"] == ["***", "***"]
    created = audit_for(db, world.p1)[0]
    assert created.changes["password_hash"] == "***"
    assert "a-new-secret-hash" not in str([e.changes for e in audit_for(db, world.p1)])


def test_audit_row_and_change_are_one_transaction(db, world):
    world.c1.full_name = "Never saved"
    db.flush()
    db.rollback()
    assert db.get(m.Child, world.c1.id).full_name == "c1"
    assert all(e.changes.get("full_name") != ["c1", "Never saved"] for e in audit_for(db, world.c1))


def test_non_record_tables_are_not_audited(db, world):
    db.add(m.RefreshToken(user_id=world.p1.id, family_id=uuid.uuid4(), token_hash="h" * 64,
                          expires_at=m._now()))
    db.commit()
    assert db.scalar(select(AuditLog.id).where(AuditLog.resource_type == "refresh_tokens")) is None


# --- unique values are only unique among live rows ----------------------------------------

def test_deleted_user_frees_the_email(db, world, make_user):
    victim = make_user("dup@x", m.UserRole.PADRE, world.e1.id)
    with pytest.raises(IntegrityError):
        make_user("dup@x", m.UserRole.PADRE, world.e1.id)
    db.rollback()
    victim = db.get(m.User, victim.id)
    victim.status = RecordStatus.DELETED
    db.commit()
    assert make_user("dup@x", m.UserRole.PADRE, world.e1.id).email == "dup@x"


def test_deleted_salon_frees_its_name(db, world):
    dup = lambda: m.Salon(id=uuid.uuid4(), estancia_id=world.e1.id, name="S1", group_type=m.GroupType.L)
    db.add(dup())
    with pytest.raises(IntegrityError):
        db.flush()
    db.rollback()
    world.s1.status = RecordStatus.DELETED
    db.add(dup())
    db.flush()


def test_archived_salon_still_blocks_its_name(db, world):
    world.s1.status = RecordStatus.ARCHIVED
    db.add(m.Salon(id=uuid.uuid4(), estancia_id=world.e1.id, name="S1", group_type=m.GroupType.L))
    with pytest.raises(IntegrityError):
        db.flush()
    db.rollback()


def test_one_daily_log_per_child_and_day_among_live_ones(db, world):
    day = m.date_type(2026, 3, 1)
    mk = lambda: m.DailyLog(id=uuid.uuid4(), child_id=world.c1.id, date=day, caregiver_name="x")
    first = mk()
    db.add(first)
    db.commit()
    db.add(mk())
    with pytest.raises(IntegrityError):
        db.flush()
    db.rollback()
    db.get(m.DailyLog, first.id).status = RecordStatus.DELETED
    db.add(mk())
    db.commit()


def test_an_admin_runs_one_live_estancia(db, world):
    db.add(m.Estancia(id=uuid.uuid4(), name="Second", admin_user_id=world.admin1.id))
    with pytest.raises(IntegrityError):
        db.flush()
    db.rollback()


# --- lifecycle helpers --------------------------------------------------------------------

def test_foreign_keys_restrict_instead_of_cascading(db, world):
    """ON DELETE RESTRICT everywhere that used to cascade (the DELETE trigger fires first, so check the catalog)."""
    cascading = db.execute(text(
        "SELECT conrelid::regclass::text FROM pg_constraint WHERE contype = 'f' AND confdeltype = 'c' "
        "AND connamespace = 'public'::regnamespace")).scalars().all()
    assert sorted(cascading) == ["device_tokens", "refresh_tokens", "user_profiles"]  # only the auth bookkeeping tables


def test_soft_delete_refuses_while_active_dependents_exist(db, world):
    with pytest.raises(HTTPException) as e:
        soft_delete(db, world.s1, blockers=[(m.Child, m.Child.salon_id)])
    assert e.value.status_code == 409 and "children" in e.value.detail


def test_archived_and_deleted_dependents_do_not_block(db, world):
    world.c1.status, world.c2.status = RecordStatus.ARCHIVED, RecordStatus.DELETED
    soft_delete(db, world.s1, blockers=[(m.Child, m.Child.salon_id)])
    assert world.s1.status == RecordStatus.DELETED


def test_soft_delete_takes_its_parts_along_and_is_idempotent(db, world):
    log = m.DailyLog(id=uuid.uuid4(), child_id=world.c1.id, date=m.date_type(2026, 1, 1), caregiver_name="x")
    act = m.ActivityRecord(id=uuid.uuid4(), daily_log_id=log.id, name="a")
    db.add_all([log, act])
    db.commit()
    soft_delete(db, log, parts=[(m.ActivityRecord, m.ActivityRecord.daily_log_id)])
    soft_delete(db, log, parts=[(m.ActivityRecord, m.ActivityRecord.daily_log_id)])
    db.commit()
    assert act.status == RecordStatus.DELETED
    assert [e.action for e in audit_for(db, act)] == [AuditAction.CREATE, AuditAction.DELETE]


def test_archive_unarchive_rules(db, world):
    archive(world.c1)
    assert world.c1.status == RecordStatus.ARCHIVED
    with pytest.raises(HTTPException):
        ensure_writable(world.c1)
    unarchive(world.c1)
    ensure_writable(world.c1)
    with pytest.raises(HTTPException):
        unarchive(world.c1)  # not archived
    soft_delete(db, world.c1)
    with pytest.raises(HTTPException):
        archive(world.c1)  # deleted rows can't be archived
