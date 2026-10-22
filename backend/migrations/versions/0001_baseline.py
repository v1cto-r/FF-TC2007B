"""baseline: the schema as created by Base.metadata.create_all before Alembic existed

Revision ID: 0001
Revises:
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = '0001'
down_revision = None
branch_labels = None
depends_on = None


ENUMS = {
    'user_role': ('ORG', 'ADMIN', 'DOCENTE', 'PADRE'),
    'group_type': ('L', 'M', 'P'),
    'amount': ('NONE', 'SOME', 'ALL', 'ASKED_FOR_MORE'),
}


def upgrade() -> None:
    bind = op.get_bind()
    for name, values in ENUMS.items():
        postgresql.ENUM(*values, name=name, schema='public').create(bind, checkfirst=True)

    op.create_table('rate_limits',
    sa.Column('key', sa.String(length=255), nullable=False),
    sa.Column('hits', sa.Integer(), nullable=False),
    sa.Column('expires_at', sa.Float(), nullable=False),
    sa.PrimaryKeyConstraint('key'),
    schema='public'
    )
    op.create_index(op.f('ix_public_rate_limits_expires_at'), 'rate_limits', ['expires_at'], unique=False, schema='public')
    op.execute('ALTER TABLE public.rate_limits SET UNLOGGED')
    op.create_table('role_actions',
    sa.Column('role', postgresql.ENUM('ORG', 'ADMIN', 'DOCENTE', 'PADRE', name='user_role', schema='public', create_type=False), nullable=False),
    sa.Column('action', sa.String(length=100), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('role', 'action'),
    schema='public'
    )
    op.create_table('users',
    sa.Column('email', sa.String(length=255), nullable=False),
    sa.Column('password_hash', sa.String(length=255), nullable=False),
    sa.Column('full_name', sa.String(length=255), nullable=False),
    sa.Column('role', postgresql.ENUM('ORG', 'ADMIN', 'DOCENTE', 'PADRE', name='user_role', schema='public', create_type=False), nullable=False),
    sa.Column('estancia_id', sa.UUID(), nullable=True),
    sa.Column('is_active', sa.Boolean(), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('email'),
    schema='public'
    )
    op.create_table('estancias',
    sa.Column('name', sa.String(length=255), nullable=False),
    sa.Column('address', sa.String(length=500), nullable=True),
    sa.Column('admin_user_id', sa.UUID(), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['admin_user_id'], ['public.users.id'], ondelete='RESTRICT'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('admin_user_id'),
    schema='public'
    )
    op.create_foreign_key('fk_users_estancia_id', 'users', 'estancias', ['estancia_id'], ['id'],
                         source_schema='public', referent_schema='public', ondelete='SET NULL')
    op.create_table('refresh_tokens',
    sa.Column('user_id', sa.UUID(), nullable=False),
    sa.Column('family_id', sa.UUID(), nullable=False),
    sa.Column('token_hash', sa.String(length=64), nullable=False),
    sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('revoked_at', sa.DateTime(timezone=True), nullable=True),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['user_id'], ['public.users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('token_hash'),
    schema='public'
    )
    op.create_index(op.f('ix_public_refresh_tokens_family_id'), 'refresh_tokens', ['family_id'], unique=False, schema='public')
    op.create_table('user_profiles',
    sa.Column('user_id', sa.UUID(), nullable=False),
    sa.Column('pending', sa.Boolean(), nullable=False),
    sa.ForeignKeyConstraint(['user_id'], ['public.users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('user_id'),
    schema='public'
    )
    op.create_table('salones',
    sa.Column('estancia_id', sa.UUID(), nullable=False),
    sa.Column('name', sa.String(length=255), nullable=False),
    sa.Column('group_type', postgresql.ENUM('L', 'M', 'P', name='group_type', schema='public', create_type=False), nullable=False),
    sa.Column('docente_id', sa.UUID(), nullable=True),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['docente_id'], ['public.users.id'], ondelete='SET NULL'),
    sa.ForeignKeyConstraint(['estancia_id'], ['public.estancias.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('estancia_id', 'name'),
    schema='public'
    )
    op.create_index(op.f('ix_public_salones_docente_id'), 'salones', ['docente_id'], unique=False, schema='public')
    op.create_index(op.f('ix_public_salones_estancia_id'), 'salones', ['estancia_id'], unique=False, schema='public')
    op.create_table('children',
    sa.Column('estancia_id', sa.UUID(), nullable=False),
    sa.Column('salon_id', sa.UUID(), nullable=True),
    sa.Column('full_name', sa.String(length=255), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['estancia_id'], ['public.estancias.id'], ondelete='CASCADE'),
    sa.ForeignKeyConstraint(['salon_id'], ['public.salones.id'], ondelete='SET NULL'),
    sa.PrimaryKeyConstraint('id'),
    schema='public'
    )
    op.create_index(op.f('ix_public_children_estancia_id'), 'children', ['estancia_id'], unique=False, schema='public')
    op.create_index(op.f('ix_public_children_salon_id'), 'children', ['salon_id'], unique=False, schema='public')
    op.create_table('notices',
    sa.Column('estancia_id', sa.UUID(), nullable=False),
    sa.Column('salon_id', sa.UUID(), nullable=True),
    sa.Column('author_id', sa.UUID(), nullable=True),
    sa.Column('name', sa.String(length=255), nullable=False),
    sa.Column('description', sa.Text(), nullable=False),
    sa.Column('date', sa.Date(), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['author_id'], ['public.users.id'], ondelete='SET NULL'),
    sa.ForeignKeyConstraint(['estancia_id'], ['public.estancias.id'], ondelete='CASCADE'),
    sa.ForeignKeyConstraint(['salon_id'], ['public.salones.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    schema='public'
    )
    op.create_index(op.f('ix_public_notices_estancia_id'), 'notices', ['estancia_id'], unique=False, schema='public')
    op.create_table('child_parents',
    sa.Column('child_id', sa.UUID(), nullable=False),
    sa.Column('parent_id', sa.UUID(), nullable=False),
    sa.Column('relationship', sa.String(length=100), nullable=True),
    sa.Column('is_primary', sa.Boolean(), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['child_id'], ['public.children.id'], ondelete='CASCADE'),
    sa.ForeignKeyConstraint(['parent_id'], ['public.users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('child_id', 'parent_id'),
    schema='public'
    )
    op.create_index(op.f('ix_public_child_parents_child_id'), 'child_parents', ['child_id'], unique=False, schema='public')
    op.create_index(op.f('ix_public_child_parents_parent_id'), 'child_parents', ['parent_id'], unique=False, schema='public')
    op.create_table('daily_logs',
    sa.Column('child_id', sa.UUID(), nullable=False),
    sa.Column('date', sa.Date(), nullable=False),
    sa.Column('caregiver_id', sa.UUID(), nullable=True),
    sa.Column('caregiver_name', sa.String(length=255), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['caregiver_id'], ['public.users.id'], ondelete='SET NULL'),
    sa.ForeignKeyConstraint(['child_id'], ['public.children.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('child_id', 'date'),
    schema='public'
    )
    op.create_index(op.f('ix_public_daily_logs_child_id'), 'daily_logs', ['child_id'], unique=False, schema='public')
    op.create_table('messages',
    sa.Column('child_id', sa.UUID(), nullable=False),
    sa.Column('author_id', sa.UUID(), nullable=True),
    sa.Column('content', sa.Text(), nullable=False),
    sa.Column('sent_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('is_aviso', sa.Boolean(), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['author_id'], ['public.users.id'], ondelete='SET NULL'),
    sa.ForeignKeyConstraint(['child_id'], ['public.children.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    schema='public'
    )
    op.create_index(op.f('ix_public_messages_child_id'), 'messages', ['child_id'], unique=False, schema='public')
    op.create_table('activity_records',
    sa.Column('daily_log_id', sa.UUID(), nullable=False),
    sa.Column('name', sa.String(length=255), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['daily_log_id'], ['public.daily_logs.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    schema='public'
    )
    op.create_index(op.f('ix_public_activity_records_daily_log_id'), 'activity_records', ['daily_log_id'], unique=False, schema='public')
    op.create_table('check_ins',
    sa.Column('daily_log_id', sa.UUID(), nullable=False),
    sa.Column('check_in_time', sa.Time(), nullable=False),
    sa.Column('dropped_off_by_name', sa.String(length=255), nullable=False),
    sa.Column('received_by_staff_name', sa.String(length=255), nullable=False),
    sa.Column('is_sick', sa.Boolean(), nullable=False),
    sa.Column('medication_left_with_prescription', sa.Boolean(), nullable=True),
    sa.Column('arrives_with_symptoms', sa.Boolean(), nullable=False),
    sa.Column('symptoms_description', sa.Text(), nullable=True),
    sa.Column('arrived_clean', sa.Boolean(), nullable=False),
    sa.Column('brought_complete_backpack', sa.Boolean(), nullable=False),
    sa.Column('good_physical_condition', sa.Boolean(), nullable=False),
    sa.Column('injury_body_part', sa.String(length=255), nullable=True),
    sa.Column('injury_description', sa.Text(), nullable=True),
    sa.Column('parents_notes', sa.Text(), nullable=True),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['daily_log_id'], ['public.daily_logs.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('daily_log_id'),
    schema='public'
    )
    op.create_table('check_outs',
    sa.Column('daily_log_id', sa.UUID(), nullable=False),
    sa.Column('check_out_time', sa.Time(), nullable=False),
    sa.Column('released_in_good_physical_condition', sa.Boolean(), nullable=False),
    sa.Column('injury_body_part', sa.String(length=255), nullable=True),
    sa.Column('injury_description', sa.Text(), nullable=True),
    sa.Column('released_clean', sa.Boolean(), nullable=False),
    sa.Column('belongings_complete', sa.Boolean(), nullable=False),
    sa.Column('notes', sa.Text(), nullable=True),
    sa.Column('picked_up_by_name', sa.String(length=255), nullable=False),
    sa.Column('released_by_staff_name', sa.String(length=255), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['daily_log_id'], ['public.daily_logs.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('daily_log_id'),
    schema='public'
    )
    op.create_table('stays',
    sa.Column('daily_log_id', sa.UUID(), nullable=False),
    sa.Column('feeding_breakfast', postgresql.ENUM('NONE', 'SOME', 'ALL', 'ASKED_FOR_MORE', name='amount', schema='public', create_type=False), nullable=False),
    sa.Column('feeding_snack', postgresql.ENUM('NONE', 'SOME', 'ALL', 'ASKED_FOR_MORE', name='amount', schema='public', create_type=False), nullable=False),
    sa.Column('feeding_lunch', postgresql.ENUM('NONE', 'SOME', 'ALL', 'ASKED_FOR_MORE', name='amount', schema='public', create_type=False), nullable=False),
    sa.Column('feeding_notes', sa.Text(), nullable=True),
    sa.Column('slept', sa.Boolean(), nullable=False),
    sa.Column('nap_minutes', sa.Integer(), nullable=True),
    sa.Column('rest_comments', sa.Text(), nullable=True),
    sa.Column('toileting_notice', sa.Boolean(), nullable=True),
    sa.Column('pee_count', sa.Integer(), nullable=False),
    sa.Column('poop_count', sa.Integer(), nullable=False),
    sa.Column('toileting_notes', sa.Text(), nullable=True),
    sa.Column('mood', sa.String(length=255), nullable=False),
    sa.Column('cried', sa.Boolean(), nullable=False),
    sa.Column('fought', sa.Boolean(), nullable=False),
    sa.Column('participated', sa.Boolean(), nullable=False),
    sa.Column('mood_notes', sa.Text(), nullable=True),
    sa.Column('had_accident', sa.Boolean(), nullable=False),
    sa.Column('accident_description', sa.Text(), nullable=True),
    sa.Column('accident_report_folio', sa.String(length=100), nullable=True),
    sa.Column('health_issue', sa.Boolean(), nullable=False),
    sa.Column('health_issue_description', sa.Text(), nullable=True),
    sa.Column('care_provided', sa.Text(), nullable=True),
    sa.Column('general_health_notes', sa.Text(), nullable=True),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['daily_log_id'], ['public.daily_logs.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    sa.UniqueConstraint('daily_log_id'),
    schema='public'
    )
    op.create_table('activity_images',
    sa.Column('activity_record_id', sa.UUID(), nullable=False),
    sa.Column('position', sa.Integer(), nullable=False),
    sa.Column('s3_key', sa.String(length=500), nullable=False),
    sa.Column('id', sa.UUID(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
    sa.ForeignKeyConstraint(['activity_record_id'], ['public.activity_records.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id'),
    schema='public'
    )
    op.create_index(op.f('ix_public_activity_images_activity_record_id'), 'activity_images', ['activity_record_id'], unique=False, schema='public')


def downgrade() -> None:
    op.drop_index(op.f('ix_public_activity_images_activity_record_id'), table_name='activity_images', schema='public')
    op.drop_table('activity_images', schema='public')
    op.drop_table('stays', schema='public')
    op.drop_table('check_outs', schema='public')
    op.drop_table('check_ins', schema='public')
    op.drop_index(op.f('ix_public_activity_records_daily_log_id'), table_name='activity_records', schema='public')
    op.drop_table('activity_records', schema='public')
    op.drop_index(op.f('ix_public_messages_child_id'), table_name='messages', schema='public')
    op.drop_table('messages', schema='public')
    op.drop_index(op.f('ix_public_daily_logs_child_id'), table_name='daily_logs', schema='public')
    op.drop_table('daily_logs', schema='public')
    op.drop_index(op.f('ix_public_child_parents_parent_id'), table_name='child_parents', schema='public')
    op.drop_index(op.f('ix_public_child_parents_child_id'), table_name='child_parents', schema='public')
    op.drop_table('child_parents', schema='public')
    op.drop_index(op.f('ix_public_notices_estancia_id'), table_name='notices', schema='public')
    op.drop_table('notices', schema='public')
    op.drop_index(op.f('ix_public_children_salon_id'), table_name='children', schema='public')
    op.drop_index(op.f('ix_public_children_estancia_id'), table_name='children', schema='public')
    op.drop_table('children', schema='public')
    op.drop_index(op.f('ix_public_salones_estancia_id'), table_name='salones', schema='public')
    op.drop_index(op.f('ix_public_salones_docente_id'), table_name='salones', schema='public')
    op.drop_table('salones', schema='public')
    op.drop_table('user_profiles', schema='public')
    op.drop_index(op.f('ix_public_refresh_tokens_family_id'), table_name='refresh_tokens', schema='public')
    op.drop_table('refresh_tokens', schema='public')
    op.drop_constraint('fk_users_estancia_id', 'users', schema='public', type_='foreignkey')
    op.drop_table('estancias', schema='public')
    op.drop_table('users', schema='public')
    op.drop_table('role_actions', schema='public')
    op.drop_index(op.f('ix_public_rate_limits_expires_at'), table_name='rate_limits', schema='public')
    op.drop_table('rate_limits', schema='public')
    bind = op.get_bind()
    for name, values in ENUMS.items():
        postgresql.ENUM(*values, name=name, schema='public').drop(bind, checkfirst=True)
