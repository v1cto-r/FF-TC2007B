"""Backend mirror of the Sonrisas app's Room database, plus users / estancias / salones.

Every id (primary and foreign) is a UUID. The client generates them, so the same id
lives on the device and here. `synced` / `sent` from Room are offline-queue flags and
are intentionally not stored on the server.
"""
import enum
import uuid
from datetime import date as date_type, datetime, time, timezone

from sqlalchemy import (
  Boolean,
  Date,
  DateTime,
  ForeignKey,
  Integer,
  String,
  Text,
  Time,
  UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import ENUM, UUID
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


# --- Organization structure --------------------------------------------------------------

class User(Entity, Base):
  """Any account: ORG, ADMIN, DOCENTE or PADRE. Children have no account."""

  __tablename__ = 'users'

  email: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)
  password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
  full_name: Mapped[str] = mapped_column(String(255), nullable=False)
  role: Mapped[UserRole] = mapped_column(_enum(UserRole, 'user_role'), nullable=False)
  # Only set for DOCENTE. ADMIN is reached through estancias.admin_user_id; PADRE through
  # child_parents -> children; ORG has no estancia.
  # use_alter: estancias.admin_user_id points back here, so one side of the cycle is added after create.
  estancia_id: Mapped[uuid.UUID | None] = _uuid_fk(
    f'{SCHEMA}.estancias.id', nullable=True, ondelete='SET NULL',
    fk={'use_alter': True, 'name': 'fk_users_estancia_id'})
  is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

  estancia: Mapped["Estancia | None"] = relationship(foreign_keys=[estancia_id], back_populates="docentes")
  children: Mapped[list["ChildParent"]] = relationship(back_populates="parent")


class Estancia(Entity, Base):
  """A daycare. Has exactly one admin, many docentes, salones and children."""

  __tablename__ = 'estancias'

  name: Mapped[str] = mapped_column(String(255), nullable=False)
  address: Mapped[str | None] = mapped_column(String(500), nullable=True)
  admin_user_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.users.id', ondelete='RESTRICT', unique=True)

  admin: Mapped[User] = relationship(foreign_keys=[admin_user_id])
  docentes: Mapped[list[User]] = relationship(foreign_keys=[User.estancia_id], back_populates="estancia")
  salones: Mapped[list["Salon"]] = relationship(back_populates="estancia", cascade="all, delete-orphan")
  children: Mapped[list["Child"]] = relationship(back_populates="estancia", cascade="all, delete-orphan")
  notices: Mapped[list["Notice"]] = relationship(back_populates="estancia", cascade="all, delete-orphan")


class Salon(Entity, Base):
  """A classroom inside an estancia: one docente, many children."""

  __tablename__ = 'salones'
  __table_args__ = (UniqueConstraint('estancia_id', 'name'), {'schema': SCHEMA})

  estancia_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.estancias.id', ondelete='CASCADE', index=True)
  name: Mapped[str] = mapped_column(String(255), nullable=False)
  group_type: Mapped[GroupType] = mapped_column(_enum(GroupType, 'group_type'), nullable=False)
  docente_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL', index=True)

  estancia: Mapped[Estancia] = relationship(back_populates="salones")
  docente: Mapped[User | None] = relationship()
  children: Mapped[list["Child"]] = relationship(back_populates="salon")


class Child(Entity, Base):
  """Room: ChildEntity. daycareCenter -> estancia, group -> salon.group_type, parentName -> child_parents."""

  __tablename__ = 'children'

  estancia_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.estancias.id', ondelete='CASCADE', index=True)
  salon_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.salones.id', nullable=True, ondelete='SET NULL', index=True)
  full_name: Mapped[str] = mapped_column(String(255), nullable=False)

  estancia: Mapped[Estancia] = relationship(back_populates="children")
  salon: Mapped[Salon | None] = relationship(back_populates="children")
  parents: Mapped[list["ChildParent"]] = relationship(back_populates="child", cascade="all, delete-orphan")
  daily_logs: Mapped[list["DailyLog"]] = relationship(back_populates="child", cascade="all, delete-orphan")
  messages: Mapped[list["Message"]] = relationship(back_populates="child", cascade="all, delete-orphan")


class ChildParent(Entity, Base):
  """Join child <-> parent user. Parent's estancias/salones are derived through children."""

  __tablename__ = 'child_parents'
  __table_args__ = (UniqueConstraint('child_id', 'parent_id'), {'schema': SCHEMA})

  child_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.children.id', ondelete='CASCADE', index=True)
  parent_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.users.id', ondelete='CASCADE', index=True)
  relationship_label: Mapped[str | None] = mapped_column('relationship', String(100), nullable=True)
  is_primary: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)

  child: Mapped[Child] = relationship(back_populates="parents")
  parent: Mapped[User] = relationship(back_populates="children")


# --- Daily log (Room: DailyLogEntity with embedded check-in / stay / check-out) -----------

class DailyLog(Entity, Base):
  """One row per child per day. The three stages live in 1:1 tables; a missing row means not done yet."""

  __tablename__ = 'daily_logs'
  __table_args__ = (UniqueConstraint('child_id', 'date'), {'schema': SCHEMA})

  child_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.children.id', ondelete='CASCADE', index=True)
  date: Mapped[date_type] = mapped_column(Date, nullable=False)
  caregiver_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL')
  caregiver_name: Mapped[str] = mapped_column(String(255), nullable=False)  # snapshot, as typed in the app

  child: Mapped[Child] = relationship(back_populates="daily_logs")
  caregiver: Mapped[User | None] = relationship()
  check_in: Mapped["CheckIn | None"] = relationship(back_populates="daily_log", uselist=False, cascade="all, delete-orphan")
  stay: Mapped["Stay | None"] = relationship(back_populates="daily_log", uselist=False, cascade="all, delete-orphan")
  check_out: Mapped["CheckOut | None"] = relationship(back_populates="daily_log", uselist=False, cascade="all, delete-orphan")
  activities: Mapped[list["ActivityRecord"]] = relationship(back_populates="daily_log", cascade="all, delete-orphan")


class CheckIn(Entity, Base):
  __tablename__ = 'check_ins'

  daily_log_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.daily_logs.id', ondelete='CASCADE', unique=True)
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

  daily_log: Mapped[DailyLog] = relationship(back_populates="check_in")


class Stay(Entity, Base):
  __tablename__ = 'stays'

  daily_log_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.daily_logs.id', ondelete='CASCADE', unique=True)
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

  daily_log: Mapped[DailyLog] = relationship(back_populates="stay")


class CheckOut(Entity, Base):
  __tablename__ = 'check_outs'

  daily_log_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.daily_logs.id', ondelete='CASCADE', unique=True)
  check_out_time: Mapped[time] = mapped_column(Time, nullable=False)
  released_in_good_physical_condition: Mapped[bool] = mapped_column(Boolean, nullable=False)
  injury_body_part: Mapped[str | None] = mapped_column(String(255), nullable=True)
  injury_description: Mapped[str | None] = mapped_column(Text, nullable=True)
  released_clean: Mapped[bool] = mapped_column(Boolean, nullable=False)
  belongings_complete: Mapped[bool] = mapped_column(Boolean, nullable=False)
  notes: Mapped[str | None] = mapped_column(Text, nullable=True)
  picked_up_by_name: Mapped[str] = mapped_column(String(255), nullable=False)
  released_by_staff_name: Mapped[str] = mapped_column(String(255), nullable=False)

  daily_log: Mapped[DailyLog] = relationship(back_populates="check_out")


class ActivityRecord(Entity, Base):
  __tablename__ = 'activity_records'

  daily_log_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.daily_logs.id', ondelete='CASCADE', index=True)
  name: Mapped[str] = mapped_column(String(255), nullable=False)

  daily_log: Mapped[DailyLog] = relationship(back_populates="activities")
  images: Mapped[list["ActivityImage"]] = relationship(
    back_populates="activity_record", cascade="all, delete-orphan", order_by="ActivityImage.position")


class ActivityImage(Entity, Base):
  """Replaces Room's '|'-joined `images` list. `s3_key` is the object key in rustfs/S3."""

  __tablename__ = 'activity_images'

  activity_record_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.activity_records.id', ondelete='CASCADE', index=True)
  position: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
  s3_key: Mapped[str] = mapped_column(String(500), nullable=False)

  activity_record: Mapped[ActivityRecord] = relationship(back_populates="images")


# --- Notices and chat -------------------------------------------------------------------

class Notice(Entity, Base):
  """Room: NoticeEntity. salon_id NULL = addressed to the whole estancia."""

  __tablename__ = 'notices'

  estancia_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.estancias.id', ondelete='CASCADE', index=True)
  salon_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.salones.id', nullable=True, ondelete='CASCADE')
  author_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL')
  name: Mapped[str] = mapped_column(String(255), nullable=False)
  description: Mapped[str] = mapped_column(Text, nullable=False)
  date: Mapped[date_type] = mapped_column(Date, nullable=False)

  estancia: Mapped[Estancia] = relationship(back_populates="notices")
  salon: Mapped[Salon | None] = relationship()
  author: Mapped[User | None] = relationship()


class Message(Entity, Base):
  """Room: MessageEntity. chatId is the child's id, so there is one chat per child."""

  __tablename__ = 'messages'

  child_id: Mapped[uuid.UUID] = _uuid_fk(f'{SCHEMA}.children.id', ondelete='CASCADE', index=True)
  author_id: Mapped[uuid.UUID | None] = _uuid_fk(f'{SCHEMA}.users.id', nullable=True, ondelete='SET NULL')
  content: Mapped[str] = mapped_column(Text, nullable=False)
  sent_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
  is_aviso: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)

  child: Mapped[Child] = relationship(back_populates="messages")
  author: Mapped[User | None] = relationship()
