"""Runs once when the API starts: makes sure Postgres has the Sonrisas schema.

1. Waits for Postgres, and creates the database itself if it doesn't exist.
2. Brings the schema to the latest Alembic revision. A database that was created by the old
   `create_all` (tables but no `alembic_version`) is stamped at the baseline first.
3. Seeds the role -> actions table when it is empty.
4. If the database has no users yet and DEBUG or TEST is set in .env, loads sample data.
5. If there is no ORG user, creates the default one (DEFAULT_ORG_EMAIL / DEFAULT_ORG_PASSWORD).

Safe to run on every start: it never touches a database that already has data.
"""
import logging
from pathlib import Path
import time
import uuid
from datetime import date, time as dtime

import bcrypt
from alembic import command
from alembic.config import Config as AlembicConfig
from sqlalchemy import create_engine, inspect, select, text
from sqlalchemy.engine import Engine, URL, make_url
from sqlalchemy.exc import OperationalError
from sqlalchemy.orm import Session

from app.auth.permissions import DEFAULT_ROLE_ACTIONS
from app.auth.security import hash_password
from app.config import Settings, settings
from app.database import models as m

log = logging.getLogger("uvicorn.error")

CONNECT_RETRIES = 30
CONNECT_DELAY_SECONDS = 2


def run(cfg: Settings = settings) -> None:
    """Entry point. Blocking, so call it with `asyncio.to_thread` from async code."""
    url = make_url(cfg.database_url)
    _ensure_database(url)

    engine = create_engine(url)
    try:
        _ensure_tables(engine)
        _seed_role_actions(engine)
        if _has_users(engine):
            log.info("starter: database already has data, nothing to do")
        elif cfg.debug or cfg.test:
            _load_sample_data(engine)
            log.info("starter: sample data loaded")
        else:
            log.info("starter: database is empty, DEBUG/TEST not set, skipping sample data")
        _ensure_default_org(engine, cfg)
    finally:
        engine.dispose()


def _ensure_database(url: URL) -> None:
    """Wait for the server, then CREATE DATABASE if it's missing (the container normally makes it)."""
    admin = create_engine(url.set(database="postgres"), isolation_level="AUTOCOMMIT")
    try:
        for attempt in range(1, CONNECT_RETRIES + 1):
            try:
                with admin.connect() as conn:
                    exists = conn.execute(
                        text("SELECT 1 FROM pg_database WHERE datname = :n"), {"n": url.database}
                    ).scalar()
                    if not exists:
                        quoted = admin.dialect.identifier_preparer.quote(url.database)
                        conn.execute(text(f"CREATE DATABASE {quoted}"))
                        log.info("starter: created database %s", url.database)
                return
            except OperationalError as e:
                if attempt == CONNECT_RETRIES:
                    raise
                log.info("starter: waiting for postgres (%d/%d): %s", attempt, CONNECT_RETRIES, e.orig)
                time.sleep(CONNECT_DELAY_SECONDS)
    finally:
        admin.dispose()


BASELINE_REVISION = "0001"
ALEMBIC_INI = Path(__file__).resolve().parent.parent / "alembic.ini"


def _alembic_config(connection) -> AlembicConfig:
    cfg = AlembicConfig(str(ALEMBIC_INI))
    cfg.set_main_option("script_location", str(ALEMBIC_INI.parent / "migrations"))
    cfg.attributes["connection"] = connection
    return cfg


def _ensure_tables(engine: Engine) -> None:
    """Stamp pre-Alembic databases, then upgrade to head."""
    tables = set(inspect(engine).get_table_names(schema=m.SCHEMA))
    with engine.begin() as conn:
        cfg = _alembic_config(conn)
        if "users" in tables and "alembic_version" not in tables:
            command.stamp(cfg, BASELINE_REVISION)
            log.info("starter: existing database stamped at %s", BASELINE_REVISION)
        command.upgrade(cfg, "head")


def _seed_role_actions(engine: Engine) -> None:
    with Session(engine) as db, db.begin():
        if db.execute(select(m.RoleAction.id).limit(1)).first() is None:
            db.add_all(m.RoleAction(role=role, action=a) for role, actions in DEFAULT_ROLE_ACTIONS.items() for a in actions)
            log.info("starter: seeded role actions")


def _ensure_default_org(engine: Engine, cfg: Settings) -> None:
    """First-time access: an ORG user whose profile is pending, so the password must be changed on first login."""
    with Session(engine) as db, db.begin():
        if db.execute(select(m.User.id).where(m.User.role == m.UserRole.ORG).limit(1)).first():
            return
        user = m.User(id=uuid.uuid4(), email=cfg.default_org_email.strip().lower(), full_name="Organización",
                      password_hash=hash_password(cfg.default_org_password), role=m.UserRole.ORG)
        db.add(user)
        db.flush()
        db.add(m.UserProfile(user_id=user.id, pending=True))
        log.info("starter: created default ORG user %s", user.email)


def _has_users(engine: Engine) -> bool:
    with Session(engine) as db:
        return db.execute(select(m.User.id).limit(1)).first() is not None


def _hash(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def _load_sample_data(engine: Engine) -> None:
    """Mirrors the app's seed (Estancia Mariposa, 5 children) plus one user per role.

    Every sample account uses the password `password`. Only runs with DEBUG/TEST on.
    """
    pw = _hash("password")

    def user(email: str, name: str, role: m.UserRole) -> m.User:
        return m.User(id=uuid.uuid4(), email=email, password_hash=pw, full_name=name, role=role)

    with Session(engine) as db, db.begin():
        org = user("org@sonrisas.test", "Dirección General", m.UserRole.ORG)
        admin = user("admin@sonrisas.test", "Admin Mariposa", m.UserRole.ADMIN)
        maestra = user("maestra@sonrisas.test", "Maestra Laura", m.UserRole.DOCENTE)
        padre = user("padre@sonrisas.test", "María López", m.UserRole.PADRE)
        others = {n: user(f"{n.split()[0].lower()}@sonrisas.test", n, m.UserRole.PADRE)
                  for n in ("Carla Ruiz", "Jorge García", "Ana Fernández")}
        db.add_all([org, admin, maestra, padre, *others.values()])
        db.flush()  # users first: estancias.admin_user_id is NOT NULL

        estancia = m.Estancia(id=uuid.uuid4(), name="Estancia Mariposa", admin_user_id=admin.id)
        db.add(estancia)
        db.flush()
        maestra.estancia_id = estancia.id
        for parent in (padre, *others.values()):
            parent.estancia_id = estancia.id

        salon = m.Salon(id=uuid.uuid4(), estancia_id=estancia.id, name="Prescolar A",
                        group_type=m.GroupType.P, docente_id=maestra.id)
        db.add(salon)
        db.flush()

        roster = [
            ("Sofía Martínez", padre), ("Regina López", padre), ("Valentina Ruiz", others["Carla Ruiz"]),
            ("Mateo García", others["Jorge García"]), ("Camila Fernández", others["Ana Fernández"]),
        ]
        children = []
        for full_name, parent in roster:
            child = m.Child(id=uuid.uuid4(), estancia_id=estancia.id, salon_id=salon.id, parent_id=parent.id,
                            full_name=full_name)
            db.add(child)
            db.flush()
            children.append(child)

        sofia = children[0]
        log_row = m.DailyLog(id=uuid.uuid4(), child_id=sofia.id, date=date.today(),
                             caregiver_id=maestra.id, caregiver_name=maestra.full_name)
        db.add(log_row)
        db.flush()
        db.add(m.CheckIn(
            id=uuid.uuid4(), daily_log_id=log_row.id, check_in_time=dtime(8, 0),
            dropped_off_by_name="María López", received_by_staff_name=maestra.full_name,
            is_sick=False, arrives_with_symptoms=False, arrived_clean=True,
            brought_complete_backpack=True, good_physical_condition=True,
        ))
        db.add(m.ActivityRecord(id=uuid.uuid4(), daily_log_id=log_row.id, name="Pintura con dedos"))

        db.add(m.Notice(id=uuid.uuid4(), estancia_id=estancia.id, author_id=admin.id,
                        name="Junta de padres", date=date.today(),
                        description="Junta general el viernes a las 9:00."))
        db.add(m.Message(id=uuid.uuid4(), child_id=sofia.id, author_id=maestra.id,
                         content="Hoy Sofía participó mucho en la actividad.",
                         sent_at=m.datetime.now(m.timezone.utc)))
