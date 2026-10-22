"""Shared fixtures. The tests run against a real Postgres (triggers, partial unique indexes and enums
don't exist in SQLite): `docker compose --profile test run --rm ff_api_test`, or point POSTGRES_* /
DB_HOST / DB_PORT at any Postgres whose database name ends in `_test`.

The schema is built once per run by `alembic upgrade head`; every test then runs inside a transaction
that is rolled back, so tests never see each other's data.
"""
import io
import os
import uuid
from contextlib import nullcontext
from datetime import datetime, timezone

for k, v in {"POSTGRES_USER": "ff", "POSTGRES_PASSWORD": "ff", "POSTGRES_DB": "ff_test",
             "S3_ACCESS_KEY": "ak", "S3_SECRET_KEY": "sk",
             "SESSION_SECRET_KEY": "session-secret-for-tests-0123456789abcdef",
             "REFRESH_SECRET_KEY": "refresh-secret-for-tests-0123456789abcdef"}.items():
    os.environ.setdefault(k, v)

import pytest
from botocore.exceptions import ClientError
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.engine import make_url
from sqlalchemy.orm import Session

from app import starter
from app.auth.security import hash_password, load_wordlist
from app.auth.token import TokenClaims, create_session_token
from app.config import settings
from app.database import models as m
from app.database.connection import SessionLocal, get_db
from app.main import app
from app.storage import StorageService
from app.storage.policy import load_policy


@pytest.fixture(autouse=True)
def _wordlist():  # per test: one test empties it on purpose
    load_wordlist()


@pytest.fixture(scope="session")
def engine():
    if not settings.postgres_db.endswith("_test"):
        pytest.exit(f"Refusing to wipe database {settings.postgres_db!r}: tests need a database whose name ends in _test",
                    returncode=2)
    url = make_url(settings.database_url)
    starter._ensure_database(url)
    eng = create_engine(url)
    with eng.begin() as conn:  # always start from nothing, so the migrations are what gets tested
        conn.execute(text("DROP SCHEMA IF EXISTS public CASCADE"))
        conn.execute(text("CREATE SCHEMA public"))
    starter._ensure_tables(eng)
    starter._seed_role_actions(eng)
    yield eng
    eng.dispose()


@pytest.fixture
def db(engine):
    conn = engine.connect()
    outer = conn.begin()
    session = Session(bind=conn, join_transaction_mode="create_savepoint", autoflush=False, expire_on_commit=False)
    yield session
    session.close()
    outer.rollback()
    conn.close()


class FakeS3:
    """Stands in for RustFS: keeps objects in memory. `put` plays the client uploading to the signed URL."""

    def __init__(self):
        self.objects: dict[tuple[str, str], tuple[bytes, str]] = {}
        self.buckets: set[str] = set()

    def head_bucket(self, Bucket):
        if Bucket not in self.buckets:
            raise ClientError({"Error": {"Code": "404"}}, "HeadBucket")

    def create_bucket(self, Bucket):
        self.buckets.add(Bucket)

    def put(self, bucket, key, data: bytes, content_type: str):
        self.objects[(bucket, key)] = (data, content_type)

    def head_object(self, Bucket, Key):
        if (Bucket, Key) not in self.objects:
            raise ClientError({"Error": {"Code": "404"}}, "HeadObject")
        data, ct = self.objects[(Bucket, Key)]
        return {"ContentLength": len(data), "ContentType": ct}

    def get_object(self, Bucket, Key, Range=None):
        data, _ = self.objects[(Bucket, Key)]
        if Range:
            start, end = Range.removeprefix("bytes=").split("-")
            data = data[int(start):int(end) + 1]
        return {"Body": io.BytesIO(data)}


@pytest.fixture
def s3():
    return FakeS3()


@pytest.fixture
def client(db, s3):
    def override():
        db.info.pop("actor_id", None)  # each request starts anonymous, like a fresh session in production
        try:
            yield db
        except Exception:
            db.rollback()
            raise

    app.dependency_overrides[get_db] = override
    app.state.session_factory = lambda: nullcontext(db)  # the chat socket's short sessions are the test's
    # Real boto3 presigning (it needs no network); only the calls that reach S3 are faked
    app.state.storage = StorageService(settings, load_policy(settings), s3_client=s3)
    yield TestClient(app, raise_server_exceptions=False)
    app.dependency_overrides.clear()
    app.state.session_factory = SessionLocal


@pytest.fixture(scope="session")
def password_hash():
    return hash_password("password")


@pytest.fixture
def make_user(db, password_hash):
    def make(email, role, estancia_id=None, active=True, pending=False, status=None):
        u = m.User(id=uuid.uuid4(), email=email, password_hash=password_hash, full_name=email, role=role,
                   estancia_id=estancia_id, is_active=active, status=status)
        db.add(u)
        db.flush()
        if pending:
            db.add(m.UserProfile(user_id=u.id, pending=True))
        db.commit()
        return u
    return make


def headers_for(db, user) -> dict:
    """A valid session token for `user`, without going through /auth/login."""
    estancia_id = user.estancia_id or db.query(m.Estancia.id).filter(m.Estancia.admin_user_id == user.id).scalar()
    claims = TokenClaims(sub=str(user.id), email=user.email, full_name=user.full_name, role=user.role.value,
                         estancia_id=str(estancia_id) if estancia_id else None, pending=False)
    return {"Authorization": f"Bearer {create_session_token(claims)}"}


@pytest.fixture
def auth(db):
    return lambda user: headers_for(db, user)


class World:
    """Two estancias with the people and children needed to test who can see / do what.

    E1: admin1, docentes d1 (salon s1) and d2 (salon s2); parents p1 (children c1, c2 in s1) and p2 (c3 in s2)
    E2: admin2, docente d3 (salon s3); parent p3 (child c4 in s3).  Plus an org user.
    """


@pytest.fixture
def world(db, make_user):
    w = World()
    R = m.UserRole
    w.org = make_user("org@x", R.ORG)
    w.admin1, w.admin2 = make_user("admin1@x", R.ADMIN), make_user("admin2@x", R.ADMIN)
    w.e1 = m.Estancia(id=uuid.uuid4(), name="E1", admin_user_id=w.admin1.id)
    w.e2 = m.Estancia(id=uuid.uuid4(), name="E2", admin_user_id=w.admin2.id)
    db.add_all([w.e1, w.e2])
    db.commit()
    w.d1, w.d2 = make_user("d1@x", R.DOCENTE, w.e1.id), make_user("d2@x", R.DOCENTE, w.e1.id)
    w.d3 = make_user("d3@x", R.DOCENTE, w.e2.id)
    w.p1, w.p2 = make_user("p1@x", R.PADRE, w.e1.id), make_user("p2@x", R.PADRE, w.e1.id)
    w.p3 = make_user("p3@x", R.PADRE, w.e2.id)
    w.s1 = m.Salon(id=uuid.uuid4(), estancia_id=w.e1.id, name="S1", group_type=m.GroupType.P, docente_id=w.d1.id)
    w.s2 = m.Salon(id=uuid.uuid4(), estancia_id=w.e1.id, name="S2", group_type=m.GroupType.M, docente_id=w.d2.id)
    w.s3 = m.Salon(id=uuid.uuid4(), estancia_id=w.e2.id, name="S3", group_type=m.GroupType.L, docente_id=w.d3.id)
    db.add_all([w.s1, w.s2, w.s3])
    db.commit()
    mk = lambda name, est, salon, parent: m.Child(id=uuid.uuid4(), estancia_id=est.id, salon_id=salon.id,
                                                  parent_id=parent.id, full_name=name)
    w.c1, w.c2 = mk("c1", w.e1, w.s1, w.p1), mk("c2", w.e1, w.s1, w.p1)
    w.c3, w.c4 = mk("c3", w.e1, w.s2, w.p2), mk("c4", w.e2, w.s3, w.p3)
    db.add_all([w.c1, w.c2, w.c3, w.c4])
    db.commit()
    return w


BUCKETS = {"ESTANCIA_LOGO": "estancias", "CHILD_DOCUMENT": "files"}


@pytest.fixture
def make_file(db):
    """A File row as if it had been uploaded (READY by default), without going through the API."""
    def make(uploader, purpose, child=None, estancia=None, state=m.UploadState.READY, content_type="image/jpeg",
             status=None, peer=None):
        fid = uuid.uuid4()
        f = m.File(id=fid, bucket=BUCKETS.get(purpose.value, "images"), key=f"{purpose.value.lower()}/{fid}.jpg",
                   purpose=purpose, kind="image", content_type=content_type, size=10, upload_state=state,
                   uploaded_by=uploader.id, child_id=child.id if child else None, peer_id=peer.id if peer else None,
                   estancia_id=estancia.id if estancia else (child.estancia_id if child else None), status=status)
        db.add(f)
        db.commit()
        return f
    return make


def now():
    return datetime.now(timezone.utc)
