"""Alembic environment. Uses the app's settings and metadata.

Tests (and the starter) can pass an open connection with
`config.attributes["connection"] = conn` to run migrations inside their own transaction.
"""
import sqlalchemy as sa
from alembic import context
from sqlalchemy import create_engine

from app.config import settings
from app.database import models

config = context.config
target_metadata = models.Base.metadata
SCHEMA = models.SCHEMA


def _include_name(name, type_, parent_names):
    # Only the app's own schema; reflection would otherwise report `public` as schema None
    return name in (None, SCHEMA) if type_ == "schema" else True


def _include_object(obj, name, type_, reflected, compare_to):
    if type_ == "table" and name == "alembic_version":
        return False
    # Reflection reports FK targets of the default schema without "public.", so every FK looks
    # changed. FK behaviour (ON DELETE, RESTRICT) is covered by tests instead.
    return type_ != "foreign_key_constraint"


def _compare_type(context, inspected_column, metadata_column, inspected_type, metadata_type):
    if isinstance(metadata_type, sa.Enum):
        return False  # same reflection quirk for schema-qualified enums
    return None


def _configure(connection) -> None:
    context.configure(
        connection=connection,
        target_metadata=target_metadata,
        include_schemas=True,
        include_name=_include_name,
        version_table_schema=SCHEMA,
        compare_type=_compare_type,
        include_object=_include_object,
    )


def run_migrations_online() -> None:
    connection = config.attributes.get("connection")
    if connection is not None:
        _configure(connection)
        with context.begin_transaction():
            context.run_migrations()
        return
    engine = create_engine(settings.database_url)
    try:
        with engine.connect() as conn:
            _configure(conn)
            with context.begin_transaction():
                context.run_migrations()
    finally:
        engine.dispose()


run_migrations_online()
