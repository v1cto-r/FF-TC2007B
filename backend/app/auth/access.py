"""What the current user may reach, resolved from the database on every request.

`Access` holds the ids of the estancias / salones / children a user can see (DELETED rows are
left out; ARCHIVED ones stay visible but read-only). ORG sees everything, so its sets stay empty
and `is_org` short-circuits every check.
"""
import uuid
from dataclasses import dataclass, field

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.token import TokenClaims
from app.database.models import Child, Estancia, RecordStatus, Salon, User


@dataclass(frozen=True)
class Access:
    user_id: uuid.UUID
    role: str
    estancias: frozenset[uuid.UUID] = field(default_factory=frozenset)
    salones: frozenset[uuid.UUID] = field(default_factory=frozenset)
    children: frozenset[uuid.UUID] = field(default_factory=frozenset)

    @property
    def is_org(self) -> bool:
        return self.role == "ORG"

    def can_see_estancia(self, estancia_id) -> bool:
        return self.is_org or estancia_id in self.estancias

    def can_see_salon(self, salon_id) -> bool:
        return self.is_org or salon_id in self.salones

    def can_see_child(self, child_id) -> bool:
        return self.is_org or child_id in self.children


def _live(model):
    return model.status.is_distinct_from(RecordStatus.DELETED)


def load_access(db: Session, claims: TokenClaims) -> Access:
    uid = uuid.UUID(claims.sub)
    role = claims.role

    def ids(stmt) -> frozenset[uuid.UUID]:
        return frozenset(db.scalars(stmt))

    if role == "PADRE":
        children = ids(select(Child.id).where(Child.parent_id == uid, _live(Child)))
        return Access(
            uid, role,
            estancias=ids(select(Estancia.id).where(
                Estancia.id == select(User.estancia_id).where(User.id == uid).scalar_subquery(), _live(Estancia))),
            salones=ids(select(Salon.id).where(
                Salon.id.in_(select(Child.salon_id).where(Child.id.in_(children))), _live(Salon))),
            children=children)

    if role == "DOCENTE":
        salones = ids(select(Salon.id).where(Salon.docente_id == uid, _live(Salon)))
        return Access(
            uid, role,
            estancias=ids(select(Estancia.id).where(
                Estancia.id == select(User.estancia_id).where(User.id == uid).scalar_subquery(), _live(Estancia))),
            salones=salones,
            children=ids(select(Child.id).where(Child.salon_id.in_(salones), _live(Child))))

    if role == "ADMIN":
        estancias = ids(select(Estancia.id).where(Estancia.admin_user_id == uid, _live(Estancia)))
        return Access(
            uid, role, estancias=estancias,
            salones=ids(select(Salon.id).where(Salon.estancia_id.in_(estancias), _live(Salon))),
            children=ids(select(Child.id).where(Child.estancia_id.in_(estancias), _live(Child))))

    return Access(uid, role)  # ORG: sees everything
