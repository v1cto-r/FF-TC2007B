"""Backend mirror of the Sonrisas app's Room database, plus users / estancias / salones.

Every id (primary and foreign) is a UUID. The client generates them, so the same id
lives on the device and here. `synced` / `sent` from Room are offline-queue flags and
are intentionally not stored on the server.

Nothing in the domain is ever hard-deleted: every `Record` has a nullable `status`
(NULL = active, ARCHIVED = read-only, DELETED = hidden) and a database trigger rejects
DELETE statements on those tables. Every change is written to `audit_log`.
"""
import enum
import uuid
from datetime import date as date_type, datetime, time, timezone

from sqlalchemy import (
  Boolean,
  Date,
  DateTime,
  Float,
  ForeignKey,
  Index,
  Integer,
  String,
  Text,
  Time,
  UniqueConstraint,
  text,
)
from sqlalchemy.dialects.postgresql import ENUM, JSONB, UUID
from sqlalchemy.orm import declarative_base, declared_attr, mapped_column, relationship, Mapped

Base = declarative_base()

SCHEMA = 'public'


def _now() -> datetime:
  return datetime.now(timezone.utc)


# --- Enums (names are sent as-is by the app: Mappers use valueOf(name)) -----------------

class UserRole(str, enum.Enum):
  ORG = 'ORG'            # sits above admins; sees every estancia
  ADMIN = 'ADMIN'        # exactly one per estancia
  DOCENTE = 'DOCENTE'
  PADRE = 'PADRE'


class GroupType(str, enum.Enum):
  L = 'L'  # Lactantes
  M = 'M'  # Maternales
  P = 'P'  # Prescolares


class Amount(str, enum.Enum):
  NONE = 'NONE'                      # Nada
  SOME = 'SOME'                      # Poco
  ALL = 'ALL'                        # Todo
  ASKED_FOR_MORE = 'ASKED_FOR_MORE'  # Pidió más


class RecordStatus(str, enum.Enum):
  """NULL (no status) means active."""
  ARCHIVED = 'ARCHIVED'  # read-only, still listed
  DELETED = 'DELETED'    # hidden from everyone but ORG; the row is kept for audit


class FilePurpose(str, enum.Enum):
  ACTIVITY_IMAGE = 'ACTIVITY_IMAGE'
  MESSAGE_ATTACHMENT = 'MESSAGE_ATTACHMENT'
  CHILD_PHOTO = 'CHILD_PHOTO'
  USER_AVATAR = 'USER_AVATAR'
  ESTANCIA_LOGO = 'ESTANCIA_LOGO'
  CHILD_DOCUMENT = 'CHILD_DOCUMENT'


class UploadState(str, enum.Enum):
  PENDING = 'PENDING'  # signed URL issued, bytes not confirmed yet
  READY = 'READY'      # the server saw the object and checked it


class AuditAction(str, enum.Enum):
  CREATE = 'CREATE'
  UPDATE = 'UPDATE'
  DELETE = 'DELETE'        # soft delete
  ARCHIVE = 'ARCHIVE'
  UNARCHIVE = 'UNARCHIVE'


def _enum(py_enum: type[enum.Enum], name: str) -> ENUM:
  return ENUM(py_enum, name=name, schema=SCHEMA)


def _uuid_fk(target: str, *, nullable: bool = False, ondelete: str | None = None, fk: dict | None = None, **kw):
  return mapped_column(UUID(as_uuid=True), ForeignKey(target, ondelete=ondelete, **(fk or {})), nullable=nullable, **kw)


class Entity:
  """Shared columns: UUID primary key plus created/updated timestamps (Room has none)."""

  id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
  created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_now)
  updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_now, onupdate=_now)

  @declared_attr.directive
  def __table_args__(cls):
    return {'schema': SCHEMA}


class Record(Entity):
  """An Entity that is soft-deleted instead of removed. Audited on every change."""

  status: Mapped[RecordStatus | None] = mapped_column(_enum(RecordStatus, 'record_status'), nullable=True)


# Partial unique index: a DELETED row no longer blocks reusing the value (email, salon name...)
def live_unique(name: str, *cols: str) -> Index:
  return Index(name, *cols, unique=True, postgresql_where=text("status IS DISTINCT FROM 'DELETED'"))


def _args(*items):
  """`__table_args__` for tables with extra constraints/indexes (the schema dict must come last)."""
  return (*items, {'schema': SCHEMA})


# --- Organization structure --------------------------------------------------------------

def _file_fk(*, name: str):
  """FK to files.id. files points back at users/estancias/children, so these are added after create."""
  return _uuid_fk(f'{SCHEMA}.files.id', nullable=True, ondelete='SET NULL', fk={'use_alter': True, 'name': name})


class User(Record, Base):
  """Any account: ORG, ADMIN, DOCENTE or PADRE. Children have no account."""

  __tablename__ = 'users'
  __table_args__ = _args(live_unique('uq_users_email_live', 'email'))

  email: Mapped[str] = mapped_column(String(255), nullable=False)
  password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
  full_name: Mapped[str] = mapped_column(String(255), nullable=False)
  role: Mapped[UserRole] = mapped_column(_enum(UserRole, 'user_role'), nullable=False)
  # Set for DOCENTE and PADRE (the estancia of the admin who created them). ADMIN is reached
  # through estancias.admin_user_id; ORG has no estancia.
  # use_alter: estancias.admin_user_id points back here, so one side of the cycle is added after create.
  estancia_id: Mapped[uuid.UUID | None] = _uuid_fk(
    f'{SCHEMA}.estancias.id', nullable=True, ondelete='SET NULL',
    fk={'use_alter': True, 'name': 'fk_users_estancia_id'})
  is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
  avatar_file_id: Mapped[uuid.UUID | None] = _file_fk(name='fk_users_avatar_file_id')

  estancia: Mapped["Estancia | None"] = relationship(foreign_keys=[estancia_id], back_populates="docentes")
  children: Mapped[list["Child"]] = relationship(back_populates="parent", foreign_keys="Child.parent_id")


class Estancia(Record, Base):
  """A daycare. Has exactly one admin, many docentes, salones and children."""

  __tablename__ = 'estancias'
  __table_args__ = _args(live_unique('uq_estancias_admin_live', 'admin_user_id'))

  name: Mapped[str] = mapped_column(String(255), nullable=False)
  address: Mapped[str | None] = mapped_column(String(500), nullable=True)
  admin_user_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.users.id', ondelete='RESTRICT')
  logo_file_id: Mapped[uuid.UUID | None] = _file_fk(name='fk_estancias_logo_file_id')

  admin: Mapped[User] = relationship(foreign_keys=[admin_user_id])
  docentes: Mapped[list[User]] = relationship(foreign_keys=[User.estancia_id], back_populates="estancia")
  salones: Mapped[list["Salon"]] = relationship(back_populates="estancia")
  children: Mapped[list["Child"]] = relationship(back_populates="estancia")
  notices: Mapped[list["Notice"]] = relationship(back_populates="estancia")


class Salon(Record, Base):
  """A classroom inside an estancia: one docente, many children."""

  __tablename__ = 'salones'
  __table_args__ = _args(live_unique('uq_salones_estancia_name_live', 'estancia_id', 'name'))

  estancia_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.estancias.id', ondelete='RESTRICT', index=True)
  name: Mapped[str] = mapped_column(String(255), nullable=False)
  group_type: Mapped[GroupType] = mapped_column(_enum(GroupType, 'group_type'), nullable=False)
  docente_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL', index=True)

  estancia: Mapped[Estancia] = relationship(back_populates="salones")
  docente: Mapped[User | None] = relationship()
  children: Mapped[list["Child"]] = relationship(back_populates="salon")


class Child(Record, Base):
  """Room: ChildEntity. daycareCenter -> estancia, group -> salon.group_type.

  One parent per child (`parent_id`); a parent can have several children."""

  __tablename__ = 'children'

  estancia_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.estancias.id', ondelete='RESTRICT', index=True)
  salon_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.salones.id', nullable=True, ondelete='SET NULL', index=True)
  parent_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL', index=True)
  full_name: Mapped[str] = mapped_column(String(255), nullable=False)
  photo_file_id: Mapped[uuid.UUID | None] = _file_fk(name='fk_children_photo_file_id')

  estancia: Mapped[Estancia] = relationship(back_populates="children")
  salon: Mapped[Salon | None] = relationship(back_populates="children")
  parent: Mapped[User | None] = relationship(back_populates="children", foreign_keys=[parent_id])


# --- Daily log (Room: DailyLogEntity with embedded check-in / stay / check-out) -----------
# The 1:1 stage tables are queried by daily_log_id (not mapped as relationships): a soft-deleted
# stage can sit next to its live replacement, which a uselist=False relationship can't express.

class DailyLog(Record, Base):
  """One row per child per day. The three stages live in 1:1 tables; a missing row means not done yet."""

  __tablename__ = 'daily_logs'
  __table_args__ = _args(live_unique('uq_daily_logs_child_date_live', 'child_id', 'date'))

  child_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.children.id', ondelete='RESTRICT', index=True)
  date: Mapped[date_type] = mapped_column(Date, nullable=False)
  caregiver_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL')
  caregiver_name: Mapped[str] = mapped_column(String(255), nullable=False)  # snapshot, as typed in the app

  child: Mapped[Child] = relationship()
  caregiver: Mapped[User | None] = relationship()


class CheckIn(Record, Base):
  __tablename__ = 'check_ins'
  __table_args__ = _args(live_unique('uq_check_ins_daily_log_live', 'daily_log_id'))

  daily_log_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.daily_logs.id', ondelete='RESTRICT')
  check_in_time: Mapped[time] = mapped_column(Time, nullable=False)
  dropped_off_by_name: Mapped[str] = mapped_column(String(255), nullable=False)
  received_by_staff_name: Mapped[str] = mapped_column(String(255), nullable=False)
  is_sick: Mapped[bool] = mapped_column(Boolean, nullable=False)
  medication_left_with_prescription: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
  arrives_with_symptoms: Mapped[bool] = mapped_column(Boolean, nullable=False)
  symptoms_description: Mapped[str | None] = mapped_column(Text, nullable=True)
  arrived_clean: Mapped[bool] = mapped_column(Boolean, nullable=False)
  brought_complete_backpack: Mapped[bool] = mapped_column(Boolean, nullable=False)
  good_physical_condition: Mapped[bool] = mapped_column(Boolean, nullable=False)
  injury_body_part: Mapped[str | None] = mapped_column(String(255), nullable=True)
  injury_description: Mapped[str | None] = mapped_column(Text, nullable=True)
  parents_notes: Mapped[str | None] = mapped_column(Text, nullable=True)


class Stay(Record, Base):
  __tablename__ = 'stays'
  __table_args__ = _args(live_unique('uq_stays_daily_log_live', 'daily_log_id'))

  daily_log_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.daily_logs.id', ondelete='RESTRICT')
  # feeding
  feeding_breakfast: Mapped[Amount] = mapped_column(_enum(Amount, 'amount'), nullable=False)
  feeding_snack: Mapped[Amount] = mapped_column(_enum(Amount, 'amount'), nullable=False)
  feeding_lunch: Mapped[Amount] = mapped_column(_enum(Amount, 'amount'), nullable=False)
  feeding_notes: Mapped[str | None] = mapped_column(Text, nullable=True)
  # rest
  slept: Mapped[bool] = mapped_column(Boolean, nullable=False)
  nap_minutes: Mapped[int | None] = mapped_column(Integer, nullable=True)
  rest_comments: Mapped[str | None] = mapped_column(Text, nullable=True)
  # toileting
  toileting_notice: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
  pee_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
  poop_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
  toileting_notes: Mapped[str | None] = mapped_column(Text, nullable=True)
  # mood
  mood: Mapped[str] = mapped_column(String(255), nullable=False)  # free text in the app
  cried: Mapped[bool] = mapped_column(Boolean, nullable=False)
  fought: Mapped[bool] = mapped_column(Boolean, nullable=False)
  participated: Mapped[bool] = mapped_column(Boolean, nullable=False)
  mood_notes: Mapped[str | None] = mapped_column(Text, nullable=True)
  # accident / health
  had_accident: Mapped[bool] = mapped_column(Boolean, nullable=False)
  accident_description: Mapped[str | None] = mapped_column(Text, nullable=True)
  accident_report_folio: Mapped[str | None] = mapped_column(String(100), nullable=True)
  health_issue: Mapped[bool] = mapped_column(Boolean, nullable=False)
  health_issue_description: Mapped[str | None] = mapped_column(Text, nullable=True)
  care_provided: Mapped[str | None] = mapped_column(Text, nullable=True)
  general_health_notes: Mapped[str | None] = mapped_column(Text, nullable=True)


class CheckOut(Record, Base):
  __tablename__ = 'check_outs'
  __table_args__ = _args(live_unique('uq_check_outs_daily_log_live', 'daily_log_id'))

  daily_log_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.daily_logs.id', ondelete='RESTRICT')
  check_out_time: Mapped[time] = mapped_column(Time, nullable=False)
  released_in_good_physical_condition: Mapped[bool] = mapped_column(Boolean, nullable=False)
  injury_body_part: Mapped[str | None] = mapped_column(String(255), nullable=True)
  injury_description: Mapped[str | None] = mapped_column(Text, nullable=True)
  released_clean: Mapped[bool] = mapped_column(Boolean, nullable=False)
  belongings_complete: Mapped[bool] = mapped_column(Boolean, nullable=False)
  notes: Mapped[str | None] = mapped_column(Text, nullable=True)
  picked_up_by_name: Mapped[str] = mapped_column(String(255), nullable=False)
  released_by_staff_name: Mapped[str] = mapped_column(String(255), nullable=False)


class ActivityRecord(Record, Base):
  __tablename__ = 'activity_records'

  daily_log_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.daily_logs.id', ondelete='RESTRICT', index=True)
  name: Mapped[str] = mapped_column(String(255), nullable=False)

  daily_log: Mapped[DailyLog] = relationship()


class ActivityImage(Record, Base):
  """Replaces Room's '|'-joined `images` list. The bytes live in rustfs; see `File`."""

  __tablename__ = 'activity_images'
  __table_args__ = _args(live_unique('uq_activity_images_file_live', 'file_id'))

  activity_record_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.activity_records.id', ondelete='RESTRICT', index=True)
  file_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.files.id', ondelete='RESTRICT')
  position: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

  activity_record: Mapped[ActivityRecord] = relationship()


# --- Notices and chat -------------------------------------------------------------------

class Notice(Record, Base):
  """Room: NoticeEntity. salon_id NULL = addressed to the whole estancia."""

  __tablename__ = 'notices'

  estancia_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.estancias.id', ondelete='RESTRICT', index=True)
  salon_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.salones.id', nullable=True, ondelete='RESTRICT', index=True)
  author_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL')
  name: Mapped[str] = mapped_column(String(255), nullable=False)
  description: Mapped[str] = mapped_column(Text, nullable=False)
  date: Mapped[date_type] = mapped_column(Date, nullable=False)

  estancia: Mapped[Estancia] = relationship(back_populates="notices")
  salon: Mapped[Salon | None] = relationship()
  author: Mapped[User | None] = relationship()


class Message(Record, Base):
  """Room: MessageEntity. chatId is the child's id, so there is one chat per child. Never edited."""

  __tablename__ = 'messages'
  __table_args__ = _args(live_unique('uq_messages_attachment_live', 'attachment_file_id'))

  child_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.children.id', ondelete='RESTRICT', index=True)
  author_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL')
  content: Mapped[str] = mapped_column(Text, nullable=False)
  sent_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
  is_aviso: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
  attachment_file_id: Mapped[uuid.UUID | None] = _file_fk(name='fk_messages_attachment_file_id')

  child: Mapped[Child] = relationship()
  author: Mapped[User | None] = relationship()


# --- Files (bytes live in rustfs/S3; this table is the registry the API authorizes from) ----

class File(Record, Base):
  """One uploaded object. PENDING until the client confirms the PUT, READY after the server checked it."""

  __tablename__ = 'files'

  bucket: Mapped[str] = mapped_column(String(63), nullable=False)
  key: Mapped[str] = mapped_column(String(500), unique=True, nullable=False)
  purpose: Mapped[FilePurpose] = mapped_column(_enum(FilePurpose, 'file_purpose'), nullable=False)
  kind: Mapped[str] = mapped_column(String(20), nullable=False)  # policy kind: image | file
  content_type: Mapped[str] = mapped_column(String(100), nullable=False)
  size: Mapped[int] = mapped_column(Integer, nullable=False)
  original_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
  upload_state: Mapped[UploadState] = mapped_column(
    _enum(UploadState, 'upload_state'), nullable=False, default=UploadState.PENDING)
  uploaded_by: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.users.id', ondelete='RESTRICT', index=True)
  # What the file belongs to. Authorization reads these, never the object key.
  estancia_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.estancias.id', nullable=True, ondelete='RESTRICT', index=True)
  child_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.children.id', nullable=True, ondelete='RESTRICT', index=True)


class ChildDocument(Record, Base):
  """A PDF / DOCX / scan (medical, legal...) a parent uploaded for their child."""

  __tablename__ = 'child_documents'
  __table_args__ = _args(live_unique('uq_child_documents_file_live', 'file_id'))

  child_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.children.id', ondelete='RESTRICT', index=True)
  file_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.files.id', ondelete='RESTRICT')
  title: Mapped[str] = mapped_column(String(255), nullable=False)


# --- Auth ----------------------------------------------------------------------------------

class RefreshToken(Entity, Base):
  """One issued refresh token (only its hash). A login starts a family; rotation keeps the family."""

  __tablename__ = 'refresh_tokens'

  user_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.users.id', ondelete='CASCADE')
  family_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False, index=True)
  token_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
  expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
  revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class RoleAction(Entity, Base):
  """An action ("child:read", "child:*", "*") a role may perform."""

  __tablename__ = 'role_actions'

  role: Mapped[UserRole] = mapped_column(_enum(UserRole, 'user_role'), nullable=False)
  action: Mapped[str] = mapped_column(String(100), nullable=False)

  @declared_attr.directive
  def __table_args__(cls):
    return (UniqueConstraint('role', 'action'), {'schema': SCHEMA})


class UserProfile(Base):
  """Only users created through the API get a row. pending=True until they change the first-time password."""

  __tablename__ = 'user_profiles'
  __table_args__ = {'schema': SCHEMA}

  user_id: Mapped[uuid.UUID] = mapped_column(
    UUID(as_uuid=True), ForeignKey(f'{SCHEMA}.users.id', ondelete='CASCADE'), primary_key=True)
  pending: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)


class RateLimit(Base):
  """Fixed-window counters, used like Redis. The starter makes this table UNLOGGED on Postgres."""

  __tablename__ = 'rate_limits'
  __table_args__ = {'schema': SCHEMA}

  key: Mapped[str] = mapped_column(String(255), primary_key=True)
  hits: Mapped[int] = mapped_column(Integer, nullable=False)
  expires_at: Mapped[float] = mapped_column(Float, nullable=False, index=True)  # unix seconds


class AuditLog(Base):
  """Append-only history of every create / update / delete on a Record (a trigger rejects UPDATE and DELETE)."""

  __tablename__ = 'audit_log'
  __table_args__ = (
    Index('ix_audit_log_resource', 'resource_type', 'resource_id'),
    {'schema': SCHEMA},
  )

  id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
  created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_now, index=True)
  actor_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='RESTRICT', index=True)  # NULL = system
  action: Mapped[AuditAction] = mapped_column(_enum(AuditAction, 'audit_action'), nullable=False)
  resource_type: Mapped[str] = mapped_column(String(64), nullable=False)  # table name
  resource_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
  changes: Mapped[dict] = mapped_column(JSONB, nullable=False, default=dict)  # {field: [old, new]}; snapshot on create
