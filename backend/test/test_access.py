"""What each role can reach (the ids behind every list and every 404)."""
from app.auth.access import load_access
from app.database.models import RecordStatus
from app.auth.token import TokenClaims


def access_of(db, user):
    return load_access(db, TokenClaims(sub=str(user.id), email=user.email, full_name="", role=user.role.value,
                                       estancia_id=None, pending=False))


def test_padre_reaches_only_their_children(db, world):
    a = access_of(db, world.p1)
    assert a.children == {world.c1.id, world.c2.id}
    assert a.salones == {world.s1.id} and a.estancias == {world.e1.id}
    assert a.can_see_child(world.c1.id) and not a.can_see_child(world.c3.id) and not a.can_see_child(world.c4.id)


def test_docente_reaches_their_own_salones(db, world):
    a = access_of(db, world.d1)
    assert a.salones == {world.s1.id} and a.children == {world.c1.id, world.c2.id}
    assert access_of(db, world.d2).children == {world.c3.id}  # same estancia, other salon


def test_admin_reaches_their_whole_estancia_and_nothing_else(db, world):
    a = access_of(db, world.admin1)
    assert a.estancias == {world.e1.id}
    assert a.salones == {world.s1.id, world.s2.id}
    assert a.children == {world.c1.id, world.c2.id, world.c3.id}
    assert access_of(db, world.admin2).children == {world.c4.id}


def test_org_sees_everything_through_the_flag(db, world):
    a = access_of(db, world.org)
    assert a.is_org and a.can_see_child(world.c4.id) and a.can_see_estancia(world.e2.id)


def test_deleted_rows_drop_out_but_archived_stay(db, world):
    world.c1.status = RecordStatus.DELETED
    world.c2.status = RecordStatus.ARCHIVED
    world.s2.status = RecordStatus.DELETED
    db.commit()
    assert access_of(db, world.p1).children == {world.c2.id}
    assert access_of(db, world.admin1).salones == {world.s1.id}
    assert access_of(db, world.admin1).children == {world.c2.id, world.c3.id}


def test_parent_without_children_reaches_nothing(db, world, make_user):
    from app.database.models import UserRole
    lonely = make_user("lonely@x", UserRole.PADRE, world.e1.id)
    a = access_of(db, lonely)
    assert not a.children and not a.salones and a.estancias == {world.e1.id}
