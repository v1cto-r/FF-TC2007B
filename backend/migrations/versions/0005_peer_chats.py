"""peer chats: messages.peer_id (DOCENTE<->ADMIN and ADMIN<->ORG), files.peer_id, ADMIN / ORG may send

Revision ID: 0005
Revises: 0004

* messages.child_id becomes nullable; a message belongs to a child's chat or to a user's (peer_id)
* files.peer_id: a photo sent in a peer chat
* ADMIN gets message:write and message:delete, ORG message:write
"""
from alembic import op
import sqlalchemy as sa

revision = '0005'
down_revision = '0004'
branch_labels = None
depends_on = None

S = 'public'

NEW_ROLE_ACTIONS = {
    'ADMIN': ['message:write', 'message:delete'],
    'ORG': ['message:write'],
}


def upgrade() -> None:
    for table in ('messages', 'files'):
        op.add_column(table, sa.Column('peer_id', sa.UUID(), nullable=True), schema=S)
        op.create_foreign_key(f'{table}_peer_id_fkey', table, 'users', ['peer_id'], ['id'],
                              source_schema=S, referent_schema=S, ondelete='RESTRICT')
        op.create_index(op.f(f'ix_public_{table}_peer_id'), table, ['peer_id'], unique=False, schema=S)
    op.alter_column('messages', 'child_id', existing_type=sa.UUID(), nullable=True, schema=S)

    # existing databases only; fresh ones are seeded by the app (it only seeds an empty table)
    for role, actions in NEW_ROLE_ACTIONS.items():
        for action in actions:
            op.execute(sa.text(
                f"INSERT INTO {S}.role_actions (id, created_at, updated_at, role, action) "
                f"SELECT gen_random_uuid(), now(), now(), CAST(:r AS {S}.user_role), :a "
                f"WHERE EXISTS (SELECT 1 FROM {S}.role_actions) "
                f"ON CONFLICT (role, action) DO NOTHING").bindparams(r=role, a=action))


def downgrade() -> None:
    # peer chat messages have no child: the downgrade fails while any exist (rows can't be deleted)
    op.alter_column('messages', 'child_id', existing_type=sa.UUID(), nullable=False, schema=S)
    for table in ('files', 'messages'):
        op.drop_index(op.f(f'ix_public_{table}_peer_id'), table_name=table, schema=S)
        op.drop_constraint(f'{table}_peer_id_fkey', table, schema=S, type_='foreignkey')
        op.drop_column(table, 'peer_id', schema=S)
    for role, actions in NEW_ROLE_ACTIONS.items():
        for action in actions:
            op.execute(sa.text(f"DELETE FROM {S}.role_actions WHERE role = CAST(:r AS {S}.user_role) AND action = :a")
                       .bindparams(r=role, a=action))
