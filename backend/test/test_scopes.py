import os

for k, v in {"POSTGRES_USER": "u", "POSTGRES_PASSWORD": "p", "POSTGRES_DB": "d",
             "S3_ACCESS_KEY": "ak", "S3_SECRET_KEY": "sk", "SECRET_KEY": "s"}.items():
    os.environ.setdefault(k, v)

import uuid

import pytest
from sqlalchemy import create_engine, event
from sqlalchemy.orm import Session

from app.database.models import Base, Child, ChildParent, Estancia, GroupType, Salon, User, UserRole
from app.storage.scopes import load_scopes


@pytest.fixture
def db():
    engine = create_engine("sqlite://").execution_options(schema_translate_map={"public": None})
    event.listen(engine, "connect", lambda c, _: c.execute("PRAGMA foreign_keys=OFF"))
    try:
        Base.metadata.create_all(engine)
    except Exception as e:  # models are Postgres-specific
        pytest.skip(f"sqlite can't build the schema: {e}")
    with Session(engine) as s:
        yield s


def test_scopes_per_role(db):
    ids = {n: uuid.uuid4() for n in "admin d1 d2 p1 p2 e1 e2 s1 s2 c1 c2 c3".split()}
    mk = lambda n, role, **kw: User(id=ids[n], email=f"{n}@x", password_hash="x", full_name=n, role=role, **kw)
    db.add_all([
        mk("admin", UserRole.ADMIN), mk("d1", UserRole.DOCENTE, estancia_id=ids["e1"]),
        mk("d2", UserRole.DOCENTE, estancia_id=ids["e1"]), mk("p1", UserRole.PADRE), mk("p2", UserRole.PADRE),
        Estancia(id=ids["e1"], name="E1", admin_user_id=ids["admin"]),
        Salon(id=ids["s1"], estancia_id=ids["e1"], name="a", group_type=list(GroupType)[0], docente_id=ids["d1"]),
        Salon(id=ids["s2"], estancia_id=ids["e1"], name="b", group_type=list(GroupType)[0], docente_id=ids["d2"]),
        Child(id=ids["c1"], estancia_id=ids["e1"], salon_id=ids["s1"], full_name="c1"),
        Child(id=ids["c2"], estancia_id=ids["e1"], salon_id=ids["s2"], full_name="c2"),
        ChildParent(child_id=ids["c1"], parent_id=ids["p1"]),
        ChildParent(child_id=ids["c2"], parent_id=ids["p2"]),
    ])
    db.commit()
    s = lambda n: str(ids[n])

    assert load_scopes(db, s("p1"), "PADRE")["child"] == {s("c1")}
    d1 = load_scopes(db, s("d1"), "DOCENTE")
    assert d1["salon"] == {s("s1")} and d1["child"] == {s("c1")} and d1["estancia"] == {s("e1")}
    a = load_scopes(db, s("admin"), "ADMIN")
    assert a["estancia"] == {s("e1")} and a["salon"] == {s("s1"), s("s2")} and a["child"] == {s("c1"), s("c2")}
    assert not any(load_scopes(db, s("p1"), "ORG").values())
