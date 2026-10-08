import uuid

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database.models import Child, ChildParent, Estancia, Salon, User, UserRole


def _ids(db: Session, stmt) -> frozenset[str]:
    return frozenset(str(i) for i in db.scalars(stmt))


def load_scopes(db: Session, user_id: str, role: str) -> dict[str, frozenset[str]]:
    """Ids the user may act on, per scope type. Resolved from the DB on every request."""
    uid = uuid.UUID(user_id)
    empty = frozenset()

    if role == UserRole.PADRE.value:
        child = _ids(db, select(ChildParent.child_id).where(ChildParent.parent_id == uid))
        return {"estancia": empty, "salon": empty, "child": child}

    if role == UserRole.DOCENTE.value:
        salon_ids = select(Salon.id).where(Salon.docente_id == uid)
        return {
            "estancia": _ids(db, select(User.estancia_id).where(User.id == uid, User.estancia_id.is_not(None))),
            "salon": _ids(db, salon_ids),
            "child": _ids(db, select(Child.id).where(Child.salon_id.in_(salon_ids))),
        }

    if role == UserRole.ADMIN.value:
        estancia_ids = select(Estancia.id).where(Estancia.admin_user_id == uid)
        return {
            "estancia": _ids(db, estancia_ids),
            "salon": _ids(db, select(Salon.id).where(Salon.estancia_id.in_(estancia_ids))),
            "child": _ids(db, select(Child.id).where(Child.estancia_id.in_(estancia_ids))),
        }

    return {"estancia": empty, "salon": empty, "child": empty}  # ORG uses the policy's "any"
