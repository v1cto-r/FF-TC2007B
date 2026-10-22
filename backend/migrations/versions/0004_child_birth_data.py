"""children: birth_date, allergies and profile_completed_at

Revision ID: 0004
Revises: 0003
"""
from alembic import op
import sqlalchemy as sa

revision = '0004'
down_revision = '0003'
branch_labels = None
depends_on = None

S = 'public'


def upgrade() -> None:
    op.add_column('children', sa.Column('birth_date', sa.Date(), nullable=True), schema=S)
    op.add_column('children', sa.Column('allergies', sa.String(length=1000), nullable=True), schema=S)
    op.add_column('children', sa.Column('profile_completed_at', sa.DateTime(timezone=True), nullable=True), schema=S)


def downgrade() -> None:
    op.drop_column('children', 'profile_completed_at', schema=S)
    op.drop_column('children', 'allergies', schema=S)
    op.drop_column('children', 'birth_date', schema=S)
