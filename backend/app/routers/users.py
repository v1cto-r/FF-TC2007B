import uuid

from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth import service
from app.auth.access import Access
from app.auth.dependency import authenticated, require_access
from app.auth.permissions import can_create_user, can_manage_user
from app.auth.security import hash_password, placeholder_password
from app.chat.hub import UNAUTHORIZED, hub
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import Child, Estancia, File, FilePurpose, Salon, User, UserProfile, UserRole
from app.rate_limit import rate_limit
from app.schemas.common import Detail
from app.schemas.users import PasswordReset, ProfileUpdate, UserCreate, UserCreated, UserOut, UserUpdate
from app.storage.rules import load_attachable, release

router = APIRouter(prefix="/users", tags=["users"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}}


def _email_taken(db: Session, email: str, except_id: uuid.UUID | None = None) -> bool:
    stmt = select(User.id).where(User.email == email, lifecycle.live(User))
    if except_id:
        stmt = stmt.where(User.id != except_id)
    return db.scalar(stmt.limit(1)) is not None


def _managed_or_404(db: Session, access: Access, user_id: uuid.UUID) -> User:
    """A user the caller is allowed to manage; anything else looks like it doesn't exist."""
    target = lifecycle.get_or_404(db, User, user_id, access)
    if not can_manage_user(access, target):
        raise HTTPException(404, "User not found")
    return target


@router.post("", status_code=201, response_model=UserCreated, responses={**ERRORS, 400: {"model": Detail}},
             dependencies=[Depends(rate_limit("create-user", 20, 60))])
def create_user(body: UserCreate, access: Access = Depends(require_access("user:create")),
                db: Session = Depends(get_db)):
    """Creates the account with a first-time password and a pending profile. The password is shown only here.
    ORG creates ADMINs; an ADMIN creates the DOCENTEs and PADREs of its estancia."""
    if not can_create_user(access.role, body.role.value):
        raise HTTPException(403, f"{access.role} can't create {body.role.value} users")
    email = body.email.strip().lower()
    if _email_taken(db, email):
        raise HTTPException(409, "Email already registered")
    estancia_id = None
    if body.role in (UserRole.DOCENTE, UserRole.PADRE):  # they belong to the creating admin's estancia
        estancia_id = next(iter(access.estancias), None)
        if estancia_id is None:
            raise HTTPException(400, "Your admin account has no estancia yet")
        lifecycle.ensure_writable(db.get(Estancia, estancia_id))  # an archived estancia takes no new people
    password = placeholder_password()
    user = User(id=uuid.uuid4(), email=email, full_name=body.full_name, role=body.role, estancia_id=estancia_id,
                password_hash=hash_password(password))
    db.add(user)
    db.flush()
    db.add(UserProfile(user_id=user.id, pending=True))
    db.commit()
    return UserCreated(id=user.id, email=user.email, role=user.role, temporary_password=password)


@router.get("", response_model=list[UserOut])
def list_users(role: UserRole | None = None, estancia_id: uuid.UUID | None = None, include_deleted: bool = False,
               access: Access = Depends(require_access("user:read")), db: Session = Depends(get_db)):
    """ORG sees every user; an ADMIN sees the DOCENTEs and PADREs of its estancia."""
    stmt = lifecycle.apply_visibility(select(User).order_by(User.full_name), User, access, include_deleted)
    if not access.is_org:
        stmt = stmt.where(User.estancia_id.in_(access.estancias), User.role.in_([UserRole.DOCENTE, UserRole.PADRE]))
    if role:
        stmt = stmt.where(User.role == role)
    if estancia_id:
        stmt = stmt.where(User.estancia_id == estancia_id)
    return db.scalars(stmt).all()


@router.get("/me", response_model=UserOut)
def get_me(access: Access = Depends(authenticated()), db: Session = Depends(get_db)):
    return db.get(User, access.user_id)


@router.patch("/me", response_model=UserOut, responses={422: {"model": Detail}})
def update_me(body: ProfileUpdate, access: Access = Depends(authenticated()), db: Session = Depends(get_db)):
    """Your own name and avatar (a file uploaded with purpose USER_AVATAR)."""
    me = db.get(User, access.user_id)
    if body.full_name is not None:
        me.full_name = body.full_name
    if "avatar_file_id" in body.model_fields_set and body.avatar_file_id != me.avatar_file_id:
        if body.avatar_file_id is not None:
            load_attachable(db, body.avatar_file_id, FilePurpose.USER_AVATAR, uploader=me.id)
        if me.avatar_file_id is not None:
            release(db.get(File, me.avatar_file_id))
        me.avatar_file_id = body.avatar_file_id
    db.commit()
    return me


@router.get("/{user_id}", response_model=UserOut, responses=ERRORS)
def get_user(user_id: uuid.UUID, access: Access = Depends(require_access("user:read")),
             db: Session = Depends(get_db)):
    return _managed_or_404(db, access, user_id)


@router.patch("/{user_id}", response_model=UserOut, responses=ERRORS)
def update_user(user_id: uuid.UUID, body: UserUpdate, access: Access = Depends(require_access("user:write")),
                db: Session = Depends(get_db)):
    target = _managed_or_404(db, access, user_id)
    lifecycle.ensure_writable(target)
    if body.email is not None:
        email = body.email.strip().lower()
        if email != target.email and _email_taken(db, email, target.id):
            raise HTTPException(409, "Email already registered")
        target.email = email
    if body.full_name is not None:
        target.full_name = body.full_name
    db.commit()
    return target


@router.post("/{user_id}/reset-password", response_model=PasswordReset, responses=ERRORS,
             dependencies=[Depends(rate_limit("reset-password", 20, 60))])
def reset_password(user_id: uuid.UUID, access: Access = Depends(require_access("user:write")),
                   db: Session = Depends(get_db)):
    """New first-time password: the user must change it at their next login, and every session is signed out."""
    target = _managed_or_404(db, access, user_id)
    lifecycle.ensure_writable(target)
    password = placeholder_password()
    target.password_hash = hash_password(password)
    profile = db.get(UserProfile, target.id)
    if profile is None:
        db.add(UserProfile(user_id=target.id, pending=True))
    else:
        profile.pending = True
    service.revoke_user_tokens(db, target.id)  # commits
    hub.close_user(target.id, UNAUTHORIZED)
    return PasswordReset(id=target.id, temporary_password=password)


def _not_self(access: Access, target: User) -> None:
    if target.id == access.user_id:
        raise HTTPException(409, "You can't archive or delete your own account")


@router.post("/{user_id}/archive", response_model=UserOut, responses=ERRORS)
def archive_user(user_id: uuid.UUID, access: Access = Depends(require_access("user:delete")),
                 db: Session = Depends(get_db)):
    """Blocks login and signs the user out. The account stays listed, read-only."""
    target = _managed_or_404(db, access, user_id)
    _not_self(access, target)
    lifecycle.archive(target)
    service.revoke_user_tokens(db, target.id)  # commits
    hub.close_user(target.id, UNAUTHORIZED)
    return target


@router.post("/{user_id}/unarchive", response_model=UserOut, responses=ERRORS)
def unarchive_user(user_id: uuid.UUID, access: Access = Depends(require_access("user:delete")),
                   db: Session = Depends(get_db)):
    target = _managed_or_404(db, access, user_id)
    lifecycle.unarchive(target)
    db.commit()
    return target


@router.delete("/{user_id}", status_code=204, responses=ERRORS)
def delete_user(user_id: uuid.UUID, access: Access = Depends(require_access("user:delete")),
                db: Session = Depends(get_db)):
    """Soft delete: blocks login, signs the user out, frees their email. Refused while an ADMIN still runs an
    estancia, a DOCENTE still has a salon, or a PADRE still has children."""
    target = _managed_or_404(db, access, user_id)
    _not_self(access, target)
    lifecycle.soft_delete(db, target, blockers=[
        (Estancia, Estancia.admin_user_id), (Salon, Salon.docente_id), (Child, Child.parent_id)])
    service.revoke_user_tokens(db, target.id)  # commits
    hub.close_user(target.id, UNAUTHORIZED)
    return Response(status_code=204)
