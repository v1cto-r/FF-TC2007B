"""The Alembic migrations, tested on a scratch database of their own (never the one other tests share)."""
import importlib.util
import uuid
from pathlib import Path

import pytest
from alembic import command
from sqlalchemy import create_engine, inspect, text
from sqlalchemy.engine import make_url

from app import starter
from app.auth.permissions import DEFAULT_ROLE_ACTIONS
from app.config import settings

VERSIONS = Path(__file__).resolve().parent.parent / "migrations/versions"
GRANTING_MIGRATIONS = ["0002_soft_delete_audit_files.py", "0005_peer_chats.py", "0006_pickup_authorizations.py"]  # the ones that add role_actions


@pytest.fixture
def scratch():
    base = make_url(settings.database_url)
    name = f"{base.database}_migrations"
    admin = create_engine(base.set(database="postgres"), isolation_level="AUTOCOMMIT")
    with admin.connect() as c:
        c.execute(text(f'DROP DATABASE IF EXISTS "{name}" WITH (FORCE)'))
        c.execute(text(f'CREATE DATABASE "{name}"'))
    engine = create_engine(base.set(database=name))
    yield engine
    engine.dispose()
    with admin.connect() as c:
        c.execute(text(f'DROP DATABASE IF EXISTS "{name}" WITH (FORCE)'))
    admin.dispose()


def upgrade(engine, rev="head"):
    with engine.begin() as conn:
        command.upgrade(starter._alembic_config(conn), rev)


def downgrade(engine, rev):
    with engine.begin() as conn:
        command.downgrade(starter._alembic_config(conn), rev)


def q(engine, sql, **params):
    with engine.connect() as conn:
        return conn.execute(text(sql), params).all()


def table_names(engine):
    return set(inspect(engine).get_table_names(schema="public"))


def test_models_and_migrations_agree(scratch):
    """If someone edits a model without writing a migration, this fails."""
    upgrade(scratch)
    with scratch.begin() as conn:
        command.check(starter._alembic_config(conn))


def test_full_downgrade_and_upgrade_again(scratch):
    upgrade(scratch)
    downgrade(scratch, "base")
    assert table_names(scratch) <= {"alembic_version"}
    upgrade(scratch)
    assert {"files", "audit_log", "child_documents"} <= table_names(scratch)


LEGACY = {k: uuid.uuid4() for k in "admin docente padre estancia salon child log activity image".split()}


def seed_legacy(engine):
    """Data as the 0001 schema (and the old API) would have left it."""
    L = LEGACY
    with engine.begin() as c:
        def ins(table, **cols):
            names = ", ".join(cols)
            vals = ", ".join(f":{k}" for k in cols)
            c.execute(text(f"INSERT INTO public.{table} ({names}, created_at, updated_at) "
                           f"VALUES ({vals}, now(), now())"), cols)

        for key, role in (("admin", "ADMIN"), ("docente", "DOCENTE"), ("padre", "PADRE")):
            ins("users", id=L[key], email=f"{key}@x", password_hash="x", full_name=key, role=role, is_active=True)
        ins("estancias", id=L["estancia"], name="E", admin_user_id=L["admin"])
        ins("salones", id=L["salon"], estancia_id=L["estancia"], name="S", group_type="P", docente_id=L["docente"])
        ins("children", id=L["child"], estancia_id=L["estancia"], salon_id=L["salon"], full_name="Kid")
        ins("child_parents", id=uuid.uuid4(), child_id=L["child"], parent_id=L["padre"], is_primary=True)
        ins("daily_logs", id=L["log"], child_id=L["child"], date="2026-01-01", caregiver_name="Seño")
        ins("activity_records", id=L["activity"], daily_log_id=L["log"], name="Pintura")
        ins("activity_images", id=L["image"], activity_record_id=L["activity"], position=0, s3_key="old/photo.png")
        ins("role_actions", id=uuid.uuid4(), role="PADRE", action="child:read")


def test_existing_data_is_carried_over_by_0002(scratch):
    L = LEGACY
    upgrade(scratch, "0001")
    seed_legacy(scratch)
    upgrade(scratch, "0002")

    # one parent per child, and the parent now belongs to the child's estancia
    assert q(scratch, "SELECT parent_id FROM public.children")[0][0] == L["padre"]
    assert q(scratch, "SELECT estancia_id FROM public.users WHERE id = :i", i=L["padre"])[0][0] == L["estancia"]
    assert "child_parents" not in table_names(scratch)

    # the photo's key became a files row (uploaded by the log's caregiver, else the estancia admin)
    f = q(scratch, "SELECT key, content_type, upload_state, purpose, uploaded_by, child_id, estancia_id, bucket "
                   "FROM public.files")[0]
    assert tuple(f) == ("old/photo.png", "image/png", "READY", "ACTIVITY_IMAGE", L["admin"], L["child"],
                        L["estancia"], "images")
    assert q(scratch, "SELECT file_id FROM public.activity_images")[0][0] == q(scratch, "SELECT id FROM public.files")[0][0]
    assert "s3_key" not in {c["name"] for c in inspect(scratch).get_columns("activity_images", schema="public")}

    # nothing is deleted or archived by the migration
    assert q(scratch, "SELECT count(*) FROM public.users WHERE status IS NOT NULL")[0][0] == 0


def test_downgrade_restores_the_old_shape(scratch):
    L = LEGACY
    upgrade(scratch, "0001")
    seed_legacy(scratch)
    upgrade(scratch)
    downgrade(scratch, "0001")

    assert q(scratch, "SELECT child_id, parent_id, is_primary FROM public.child_parents")[0] == (L["child"], L["padre"], True)
    assert q(scratch, "SELECT s3_key FROM public.activity_images")[0][0] == "old/photo.png"
    assert "files" not in table_names(scratch)


def test_hard_deletes_work_again_after_downgrade(scratch):
    upgrade(scratch)
    downgrade(scratch, "0001")
    with scratch.begin() as c:  # the trigger is gone with the migration
        c.execute(text("INSERT INTO public.users (id, email, password_hash, full_name, role, is_active, created_at, "
                       "updated_at) VALUES (gen_random_uuid(), 'a@x', 'x', 'A', 'ORG', true, now(), now())"))
        assert c.execute(text("DELETE FROM public.users")).rowcount == 1


def test_new_role_actions_reach_existing_databases_only(scratch):
    upgrade(scratch, "0001")
    seed_legacy(scratch)  # includes one role_action: the table is "in use"
    upgrade(scratch)
    padre = {a for (a,) in q(scratch, "SELECT action FROM public.role_actions WHERE role = 'PADRE'")}
    assert {a for a in DEFAULT_ROLE_ACTIONS[next(r for r in DEFAULT_ROLE_ACTIONS if r.value == "PADRE")]} <= padre


def test_fresh_database_is_left_for_the_app_to_seed(scratch):
    upgrade(scratch)
    assert q(scratch, "SELECT count(*) FROM public.role_actions")[0][0] == 0


def test_pre_alembic_database_is_stamped_then_upgraded(scratch):
    """A database made by the old create_all (tables, but no alembic_version) must not be re-created."""
    upgrade(scratch, "0001")
    seed_legacy(scratch)
    with scratch.begin() as c:
        c.execute(text("DROP TABLE public.alembic_version"))
    starter._ensure_tables(scratch)
    assert q(scratch, "SELECT version_num FROM public.alembic_version")[0][0] == "0006"
    assert q(scratch, "SELECT parent_id FROM public.children")[0][0] == LEGACY["padre"]  # data kept
    starter._ensure_tables(scratch)  # running again changes nothing


def test_code_and_migrations_grant_the_same_actions():
    granted: dict[str, set[str]] = {}
    for name in GRANTING_MIGRATIONS:
        spec = importlib.util.spec_from_file_location(name.removesuffix(".py"), VERSIONS / name)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        for role, actions in mod.NEW_ROLE_ACTIONS.items():
            granted.setdefault(role, set()).update(actions)
    assert {r.value: set(a) for r, a in DEFAULT_ROLE_ACTIONS.items()} == granted
