"""device_tokens: FCM registration tokens for push notifications

Revision ID: 0003
Revises: 0002
"""
from alembic import op
import sqlalchemy as sa

revision = '0003'
down_revision = '0002'
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table('device_tokens',
    sa.Column('user_id', sa.UUID(), nullable=False),
    sa.Column('token', sa.String(length=512), nullable=False),
    sa.Column('platform', sa.String(length=20), nullable=False),
    sa.Column('last_seen', sa.DateTime(timezone=True), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['user_id'], ['public.users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('token'),
    schema='public'
    )
    op.create_index(op.f('ix_public_device_tokens_user_id'), 'device_tokens', ['user_id'], unique=False, schema='public')


def downgrade() -> None:
    op.drop_index(op.f('ix_public_device_tokens_user_id'), table_name='device_tokens', schema='public')
    op.drop_table('device_tokens', schema='public')
