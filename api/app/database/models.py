import enum
import uuid
from datetime import datetime, timezone

from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Table,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import ENUM, JSONB, UUID
from sqlalchemy.orm import declarative_base, relationship, Mapped, mapped_column

Base = declarative_base()


class Users(Base):
  """One configured map (a `slug`), the root of the sources/layers/filters tree."""
  
  __tablename__ = 'map_configs'
  __table_args__ = { 'schema': 'public' }
  
  id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
  slug: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)
  display_name: Mapped[str] = mapped_column(String(255), nullable=False)
  subtitle: Mapped[str | None] = mapped_column(String(255), nullable=True)
  handle: Mapped[str | None] = mapped_column(String(255), nullable=True)
  data_hash: Mapped[str | None] = mapped_column(String(255), nullable=True)
  charts: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
  initial_center: Mapped[str] = mapped_column(Geometry(geometry_type='POINT', srid=4326), nullable=False)
  initial_zoom: Mapped[int] = mapped_column(Integer, nullable=False)
  created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
  updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc),
                                               onupdate=lambda: datetime.now(timezone.utc))
  
  sources: Mapped[list["Source"]] = relationship(back_populates="map_config", cascade="all, delete-orphan")
  filters: Mapped[list["Filter"]] = relationship(back_populates="map_config", cascade="all, delete-orphan")

