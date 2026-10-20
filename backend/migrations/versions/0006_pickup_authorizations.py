"""pickup authorizations, the ID photo at check-out, the afternoon snack

Revision ID: 0006
Revises: 0005

* new table pickup_authorizations (a Record: soft-deleted, audited, no hard deletes)
* check_outs.pickup_authorization_id and check_outs.id_photo_file_id
* stays.feeding_afternoon_snack (null on existing rows)
* file purpose PICKUP_ID_PHOTO
* PADRE gets pickup:*, the other roles pickup:read
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = '0006'
down_revision = '0005'
branch_labels = None
depends_on = None

S = 'public'

NEW_ROLE_ACTIONS = {
    'ORG': ['pickup:read'],
    'ADMIN': ['pickup:read'],
    'DOCENTE': ['pickup:read'],
    'PADRE': ['pickup:*'],
}


def _enum(name: str, *values: str) -> postgresql.ENUM:
    return postgresql.ENUM(*values, name=name, schema=S, create_type=False)


def upgrade() -> None:
    # New enum values can't be used in the transaction that adds them; nothing below uses this one
    op.execute(f"ALTER TYPE {S}.file_purpose ADD VALUE IF NOT EXISTS 'PICKUP_ID_PHOTO'")
    postgresql.ENUM('THIS_WEEK', 'RECURRING', name='pickup_kind', schema=S).create(op.get_bind(), checkfirst=True)

    op.create_table(
        'pickup_authorizations',
        sa.Column('child_id', sa.UUID(), nullable=False),
        sa.Column('created_by', sa.UUID(), nullable=True),
        sa.Column('kind', _enum('pickup_kind', 'THIS_WEEK', 'RECURRING'), nullable=False),
        sa.Column('start_date', sa.Date(), nullable=False),
        sa.Column('weekdays', sa.Integer(), nullable=False),
        sa.Column('authorized_person_name', sa.String(255), nullable=False),
        sa.Column('relationship', sa.String(255), nullable=False),
        sa.Column('phone', sa.String(50), nullable=False),
        sa.Column('identification_type', sa.String(50), nullable=False),
        sa.Column('notes', sa.Text(), nullable=True),
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('status', _enum('record_status', 'ARCHIVED', 'DELETED'), nullable=True),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['child_id'], [f'{S}.children.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['created_by'], [f'{S}.users.id'], ondelete='SET NULL'),
        schema=S,
    )
    op.create_index(op.f('ix_public_pickup_authorizations_child_id'), 'pickup_authorizations', ['child_id'],
                    unique=False, schema=S)
    op.execute(f"CREATE TRIGGER trg_pickup_authorizations_no_delete BEFORE DELETE ON {S}.pickup_authorizations "
               f"FOR EACH ROW EXECUTE FUNCTION {S}.forbid_delete()")
    op.execute(f"CREATE TRIGGER trg_pickup_authorizations_no_truncate BEFORE TRUNCATE ON {S}.pickup_authorizations "
               f"FOR EACH STATEMENT EXECUTE FUNCTION {S}.forbid_delete()")

    op.add_column('check_outs', sa.Column('pickup_authorization_id', sa.UUID(), nullable=True), schema=S)
    op.add_column('check_outs', sa.Column('id_photo_file_id', sa.UUID(), nullable=True), schema=S)
    op.create_foreign_key('check_outs_pickup_authorization_id_fkey', 'check_outs', 'pickup_authorizations',
                          ['pickup_authorization_id'], ['id'], source_schema=S, referent_schema=S, ondelete='RESTRICT')
    op.create_foreign_key('check_outs_id_photo_file_id_fkey', 'check_outs', 'files', ['id_photo_file_id'], ['id'],
                          source_schema=S, referent_schema=S, ondelete='RESTRICT')

    op.add_column('stays', sa.Column('feeding_afternoon_snack', _enum('amount'), nullable=True), schema=S)

    # existing databases only; fresh ones are seeded by the app (it only seeds an empty table)
    for role, actions in NEW_ROLE_ACTIONS.items():
        for action in actions:
            op.execute(sa.text(
                f"INSERT INTO {S}.role_actions (id, created_at, updated_at, role, action) "
                f"SELECT gen_random_uuid(), now(), now(), CAST(:r AS {S}.user_role), :a "
                f"WHERE EXISTS (SELECT 1 FROM {S}.role_actions) "
                f"ON CONFLICT (role, action) DO NOTHING").bindparams(r=role, a=action))


def downgrade() -> None:
    # PICKUP_ID_PHOTO stays in file_purpose: Postgres can't drop an enum value. Files of that purpose are kept.
    op.drop_column('stays', 'feeding_afternoon_snack', schema=S)
    op.drop_constraint('check_outs_id_photo_file_id_fkey', 'check_outs', schema=S, type_='foreignkey')
    op.drop_constraint('check_outs_pickup_authorization_id_fkey', 'check_outs', schema=S, type_='foreignkey')
    op.drop_column('check_outs', 'id_photo_file_id', schema=S)
    op.drop_column('check_outs', 'pickup_authorization_id', schema=S)
    op.execute(f"DROP TRIGGER trg_pickup_authorizations_no_delete ON {S}.pickup_authorizations")
    op.execute(f"DROP TRIGGER trg_pickup_authorizations_no_truncate ON {S}.pickup_authorizations")
    op.drop_index(op.f('ix_public_pickup_authorizations_child_id'), table_name='pickup_authorizations', schema=S)
    op.drop_table('pickup_authorizations', schema=S)
    postgresql.ENUM(name='pickup_kind', schema=S).drop(op.get_bind(), checkfirst=True)
    for role, actions in NEW_ROLE_ACTIONS.items():
        for action in actions:
            op.execute(sa.text(f"DELETE FROM {S}.role_actions WHERE role = CAST(:r AS {S}.user_role) AND action = :a")
                       .bindparams(r=role, a=action))
