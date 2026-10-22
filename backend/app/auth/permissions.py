from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.database.models import RoleAction, User, UserRole

# Seeded into role_actions the first time the table is empty. After that the DB is the source of truth.
# Verbs: read, write (PUT / archive / unarchive), delete, create (only for users); "resource:*" = all of them.
# This is the coarse gate. Which rows a user may touch is decided by Access (scopes) and permissions.py.
DEFAULT_ROLE_ACTIONS = {
    UserRole.ORG: ["estancia:*", "user:*", "session:revoke", "salon:read", "child:read", "daily_log:read",
                   "notice:read", "message:read", "message:write", "file:*", "document:read", "pickup:read"],
    UserRole.ADMIN: ["estancia:read", "estancia:write", "salon:*", "child:*", "daily_log:read", "daily_log:write",
                     "notice:*", "message:read", "message:write", "message:delete", "user:*", "session:revoke", "file:*",
                     "document:read", "pickup:read"],
    UserRole.DOCENTE: ["estancia:read", "salon:read", "child:read", "daily_log:*", "notice:*", "message:read", "message:write",
                       "file:*", "document:read", "pickup:read"],
    UserRole.PADRE: ["estancia:read", "salon:read", "child:read", "daily_log:read", "notice:read", "message:read",
                     "message:write", "message:delete", "file:*", "document:*", "pickup:*"],
}

# Who may create which roles
CREATABLE_ROLES = {"ORG": {"ADMIN"}, "ADMIN": {"DOCENTE", "PADRE"}}


def role_actions(db: Session, role: str) -> set[str]:
    return set(db.scalars(select(RoleAction.action).where(RoleAction.role == UserRole(role))))


def action_allowed(action: str, granted: set[str]) -> bool:
    """Exact match, "resource:*" or "*"."""
    resource = action.split(":")[0]
    return action in granted or f"{resource}:*" in granted or "*" in granted


def can_create_user(role: str, new_role: str) -> bool:
    return new_role in CREATABLE_ROLES.get(role, set())


def can_manage_user(access: Access, target: User) -> bool:
    """ORG manages everyone. ADMIN manages the DOCENTEs and PADREs of its own estancia."""
    if access.is_org:
        return True
    if access.role == "ADMIN":
        return target.role in (UserRole.DOCENTE, UserRole.PADRE) and target.estancia_id in access.estancias
    return False
