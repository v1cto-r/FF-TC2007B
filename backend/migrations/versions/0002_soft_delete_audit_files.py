"""soft delete (status), audit_log, files registry, children.parent_id

Revision ID: 0002
Revises: 0001

* every domain table gets a nullable `status` (NULL = active); a trigger rejects DELETE on them
* unique constraints become partial unique indexes that ignore DELETED rows
* ON DELETE CASCADE foreign keys become RESTRICT
* child_parents is replaced by children.parent_id (one parent per child)
* PADRE users get users.estancia_id
* activity_images.s3_key becomes a reference to the new files table
* new tables: files, child_documents, audit_log (append-only)
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = '0002'
down_revision = '0001'
branch_labels = None
depends_on = None

S = 'public'

EXISTING_SOFT = ['users', 'estancias', 'salones', 'children', 'daily_logs', 'check_ins', 'stays',
                 'check_outs', 'activity_records', 'activity_images', 'notices', 'messages']
ALL_SOFT = EXISTING_SOFT + ['files', 'child_documents']

ENUMS = {
    'record_status': ('ARCHIVED', 'DELETED'),
    'file_purpose': ('ACTIVITY_IMAGE', 'MESSAGE_ATTACHMENT', 'CHILD_PHOTO', 'USER_AVATAR',
                     'ESTANCIA_LOGO', 'CHILD_DOCUMENT'),
    'upload_state': ('PENDING', 'READY'),
    'audit_action': ('CREATE', 'UPDATE', 'DELETE', 'ARCHIVE', 'UNARCHIVE'),
}

# (table, column, old fk name, referenced table, nullable, old ondelete -> RESTRICT)
CASCADE_FKS = [
    ('salones', 'estancia_id', 'salones_estancia_id_fkey', 'estancias'),
    ('children', 'estancia_id', 'children_estancia_id_fkey', 'estancias'),
    ('notices', 'estancia_id', 'notices_estancia_id_fkey', 'estancias'),
    ('notices', 'salon_id', 'notices_salon_id_fkey', 'salones'),
    ('daily_logs', 'child_id', 'daily_logs_child_id_fkey', 'children'),
    ('messages', 'child_id', 'messages_child_id_fkey', 'children'),
    ('activity_records', 'daily_log_id', 'activity_records_daily_log_id_fkey', 'daily_logs'),
    ('check_ins', 'daily_log_id', 'check_ins_daily_log_id_fkey', 'daily_logs'),
    ('check_outs', 'daily_log_id', 'check_outs_daily_log_id_fkey', 'daily_logs'),
    ('stays', 'daily_log_id', 'stays_daily_log_id_fkey', 'daily_logs'),
    ('activity_images', 'activity_record_id', 'activity_images_activity_record_id_fkey', 'activity_records'),
]

# old unique constraint -> (new partial unique index, columns)
UNIQUES = [
    ('users', 'users_email_key', 'uq_users_email_live', ['email']),
    ('estancias', 'estancias_admin_user_id_key', 'uq_estancias_admin_live', ['admin_user_id']),
    ('salones', 'salones_estancia_id_name_key', 'uq_salones_estancia_name_live', ['estancia_id', 'name']),
    ('daily_logs', 'daily_logs_child_id_date_key', 'uq_daily_logs_child_date_live', ['child_id', 'date']),
    ('check_ins', 'check_ins_daily_log_id_key', 'uq_check_ins_daily_log_live', ['daily_log_id']),
    ('stays', 'stays_daily_log_id_key', 'uq_stays_daily_log_live', ['daily_log_id']),
    ('check_outs', 'check_outs_daily_log_id_key', 'uq_check_outs_daily_log_live', ['daily_log_id']),
]

LIVE = sa.text("status IS DISTINCT FROM 'DELETED'")

# Additive: only inserts what is missing, never removes a grant someone configured by hand
NEW_ROLE_ACTIONS = {
    'ORG': ['estancia:*', 'user:*', 'session:revoke', 'salon:read', 'child:read', 'daily_log:read',
            'notice:read', 'message:read', 'file:*', 'document:read'],
    'ADMIN': ['estancia:read', 'estancia:write', 'salon:*', 'child:*', 'daily_log:read', 'daily_log:write',
              'notice:*', 'message:read', 'user:*', 'session:revoke', 'file:*', 'document:read'],
    'DOCENTE': ['estancia:read', 'salon:read', 'child:read', 'daily_log:*', 'notice:*', 'message:read', 'message:write',
                'file:*', 'document:read'],
    'PADRE': ['estancia:read', 'salon:read', 'child:read', 'daily_log:read', 'notice:read', 'message:read',
              'message:write', 'message:delete', 'file:*', 'document:*'],
}


def _enum(name: str) -> postgresql.ENUM:
    return postgresql.ENUM(*ENUMS[name], name=name, schema=S, create_type=False)


def _base_cols() -> list[sa.Column]:
    return [
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('status', _enum('record_status'), nullable=True),
    ]


def upgrade() -> None:
    bind = op.get_bind()
    for name, values in ENUMS.items():
        postgresql.ENUM(*values, name=name, schema=S).create(bind, checkfirst=True)

    # --- status on the existing domain tables
    for t in EXISTING_SOFT:
        op.add_column(t, sa.Column('status', _enum('record_status'), nullable=True), schema=S)

    # --- new tables
    op.create_table(
        'files',
        sa.Column('bucket', sa.String(63), nullable=False),
        sa.Column('key', sa.String(500), nullable=False),
        sa.Column('purpose', _enum('file_purpose'), nullable=False),
        sa.Column('kind', sa.String(20), nullable=False),
        sa.Column('content_type', sa.String(100), nullable=False),
        sa.Column('size', sa.Integer(), nullable=False),
        sa.Column('original_name', sa.String(255), nullable=True),
        sa.Column('upload_state', _enum('upload_state'), nullable=False),
        sa.Column('uploaded_by', sa.UUID(), nullable=False),
        sa.Column('estancia_id', sa.UUID(), nullable=True),
        sa.Column('child_id', sa.UUID(), nullable=True),
        *_base_cols(),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('key'),
        sa.ForeignKeyConstraint(['uploaded_by'], [f'{S}.users.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['estancia_id'], [f'{S}.estancias.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['child_id'], [f'{S}.children.id'], ondelete='RESTRICT'),
        schema=S,
    )
    for col in ('uploaded_by', 'estancia_id', 'child_id'):
        op.create_index(f'ix_{S}_files_{col}', 'files', [col], schema=S)

    op.create_table(
        'child_documents',
        sa.Column('child_id', sa.UUID(), nullable=False),
        sa.Column('file_id', sa.UUID(), nullable=False),
        sa.Column('title', sa.String(255), nullable=False),
        *_base_cols(),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['child_id'], [f'{S}.children.id'], ondelete='RESTRICT'),
        sa.ForeignKeyConstraint(['file_id'], [f'{S}.files.id'], ondelete='RESTRICT'),
        schema=S,
    )
    op.create_index(f'ix_{S}_child_documents_child_id', 'child_documents', ['child_id'], schema=S)
    op.create_index('uq_child_documents_file_live', 'child_documents', ['file_id'], unique=True,
                    schema=S, postgresql_where=LIVE)

    op.create_table(
        'audit_log',
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('actor_id', sa.UUID(), nullable=True),
        sa.Column('action', _enum_audit(), nullable=False),
        sa.Column('resource_type', sa.String(64), nullable=False),
        sa.Column('resource_id', sa.UUID(), nullable=False),
        sa.Column('changes', postgresql.JSONB(), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.ForeignKeyConstraint(['actor_id'], [f'{S}.users.id'], ondelete='RESTRICT'),
        schema=S,
    )
    op.create_index(f'ix_{S}_audit_log_created_at', 'audit_log', ['created_at'], schema=S)
    op.create_index(f'ix_{S}_audit_log_actor_id', 'audit_log', ['actor_id'], schema=S)
    op.create_index('ix_audit_log_resource', 'audit_log', ['resource_type', 'resource_id'], schema=S)

    # --- columns that point at files
    for table, col, fk in [('users', 'avatar_file_id', 'fk_users_avatar_file_id'),
                           ('estancias', 'logo_file_id', 'fk_estancias_logo_file_id'),
                           ('children', 'photo_file_id', 'fk_children_photo_file_id'),
                           ('messages', 'attachment_file_id', 'fk_messages_attachment_file_id')]:
        op.add_column(table, sa.Column(col, sa.UUID(), nullable=True), schema=S)
        op.create_foreign_key(fk, table, 'files', [col], ['id'], source_schema=S, referent_schema=S,
                              ondelete='SET NULL')
    op.create_index('uq_messages_attachment_live', 'messages', ['attachment_file_id'], unique=True,
                    schema=S, postgresql_where=LIVE)

    # --- child_parents -> children.parent_id (+ PADRE users get an estancia)
    op.add_column('children', sa.Column('parent_id', sa.UUID(), nullable=True), schema=S)
    op.create_foreign_key('children_parent_id_fkey', 'children', 'users', ['parent_id'], ['id'],
                          source_schema=S, referent_schema=S, ondelete='SET NULL')
    op.create_index(f'ix_{S}_children_parent_id', 'children', ['parent_id'], schema=S)
    op.execute(f"""
        UPDATE {S}.children c SET parent_id = (
            SELECT cp.parent_id FROM {S}.child_parents cp WHERE cp.child_id = c.id
            ORDER BY cp.is_primary DESC, cp.created_at LIMIT 1)
    """)
    op.execute(f"""
        UPDATE {S}.users u SET estancia_id = c.estancia_id
        FROM {S}.children c WHERE c.parent_id = u.id AND u.role = 'PADRE' AND u.estancia_id IS NULL
    """)
    op.drop_table('child_parents', schema=S)

    # --- activity_images.s3_key -> files
    op.add_column('activity_images', sa.Column('file_id', sa.UUID(), nullable=True), schema=S)
    op.execute(f"""
        INSERT INTO {S}.files (id, created_at, updated_at, bucket, key, purpose, kind, content_type, size,
                               upload_state, uploaded_by, estancia_id, child_id)
        SELECT ai.id, now(), now(), 'images', ai.s3_key, 'ACTIVITY_IMAGE', 'image',
               CASE WHEN lower(ai.s3_key) LIKE '%.png' THEN 'image/png'
                    WHEN lower(ai.s3_key) LIKE '%.webp' THEN 'image/webp'
                    ELSE 'image/jpeg' END,
               0, 'READY', COALESCE(dl.caregiver_id, e.admin_user_id), c.estancia_id, c.id
        FROM {S}.activity_images ai
        JOIN {S}.activity_records ar ON ar.id = ai.activity_record_id
        JOIN {S}.daily_logs dl ON dl.id = ar.daily_log_id
        JOIN {S}.children c ON c.id = dl.child_id
        JOIN {S}.estancias e ON e.id = c.estancia_id
    """)
    op.execute(f"UPDATE {S}.activity_images SET file_id = id")
    op.alter_column('activity_images', 'file_id', nullable=False, schema=S)
    op.create_foreign_key('activity_images_file_id_fkey', 'activity_images', 'files', ['file_id'], ['id'],
                          source_schema=S, referent_schema=S, ondelete='RESTRICT')
    op.drop_column('activity_images', 's3_key', schema=S)
    op.create_index('uq_activity_images_file_live', 'activity_images', ['file_id'], unique=True,
                    schema=S, postgresql_where=LIVE)

    # --- uniques that ignore DELETED rows
    for table, old, new, cols in UNIQUES:
        op.drop_constraint(old, table, schema=S, type_='unique')
        op.create_index(new, table, cols, unique=True, schema=S, postgresql_where=LIVE)

    # --- CASCADE -> RESTRICT
    for table, col, fk, target in CASCADE_FKS:
        op.drop_constraint(fk, table, schema=S, type_='foreignkey')
        op.create_foreign_key(fk, table, target, [col], ['id'], source_schema=S, referent_schema=S,
                              ondelete='RESTRICT')
    op.create_index(f'ix_{S}_notices_salon_id', 'notices', ['salon_id'], schema=S)

    # --- nothing is ever hard-deleted; the audit log is append-only
    op.execute(f"""
        CREATE FUNCTION {S}.forbid_delete() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN
            RAISE EXCEPTION 'Hard delete is not allowed on %.%: set status = ''DELETED'' instead',
                TG_TABLE_SCHEMA, TG_TABLE_NAME USING ERRCODE = 'restrict_violation';
        END $$
    """)
    op.execute(f"""
        CREATE FUNCTION {S}.forbid_audit_change() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN
            RAISE EXCEPTION 'audit_log is append-only' USING ERRCODE = 'restrict_violation';
        END $$
    """)
    for t in ALL_SOFT:
        op.execute(f"CREATE TRIGGER trg_{t}_no_delete BEFORE DELETE ON {S}.{t} "
                   f"FOR EACH ROW EXECUTE FUNCTION {S}.forbid_delete()")
        op.execute(f"CREATE TRIGGER trg_{t}_no_truncate BEFORE TRUNCATE ON {S}.{t} "
                   f"FOR EACH STATEMENT EXECUTE FUNCTION {S}.forbid_delete()")
    op.execute(f"CREATE TRIGGER trg_audit_log_append_only BEFORE UPDATE OR DELETE ON {S}.audit_log "
               f"FOR EACH ROW EXECUTE FUNCTION {S}.forbid_audit_change()")
    op.execute(f"CREATE TRIGGER trg_audit_log_no_truncate BEFORE TRUNCATE ON {S}.audit_log "
               f"FOR EACH STATEMENT EXECUTE FUNCTION {S}.forbid_audit_change()")

    # --- permissions for the new endpoints (existing databases only; fresh ones are seeded by the app)
    for role, actions in NEW_ROLE_ACTIONS.items():
        for action in actions:
            op.execute(sa.text(
                f"INSERT INTO {S}.role_actions (id, created_at, updated_at, role, action) "
                f"SELECT gen_random_uuid(), now(), now(), CAST(:r AS {S}.user_role), :a "
                f"WHERE EXISTS (SELECT 1 FROM {S}.role_actions) "
                f"ON CONFLICT (role, action) DO NOTHING").bindparams(r=role, a=action))


def _enum_audit() -> postgresql.ENUM:
    return _enum('audit_action')


def downgrade() -> None:
    # triggers first: the data moves below would otherwise be blocked
    for t in ALL_SOFT:
        op.execute(f"DROP TRIGGER IF EXISTS trg_{t}_no_delete ON {S}.{t}")
        op.execute(f"DROP TRIGGER IF EXISTS trg_{t}_no_truncate ON {S}.{t}")
    op.execute(f"DROP TRIGGER IF EXISTS trg_audit_log_append_only ON {S}.audit_log")
    op.execute(f"DROP TRIGGER IF EXISTS trg_audit_log_no_truncate ON {S}.audit_log")

    op.drop_index(f'ix_{S}_notices_salon_id', table_name='notices', schema=S)
    for table, col, fk, target in CASCADE_FKS:
        op.drop_constraint(fk, table, schema=S, type_='foreignkey')
        op.create_foreign_key(fk, table, target, [col], ['id'], source_schema=S, referent_schema=S,
                              ondelete='CASCADE')

    for table, old, new, cols in UNIQUES:
        op.drop_index(new, table_name=table, schema=S)
        op.create_unique_constraint(old, table, cols, schema=S)

    # activity_images.file_id -> s3_key
    op.drop_index('uq_activity_images_file_live', table_name='activity_images', schema=S)
    op.add_column('activity_images', sa.Column('s3_key', sa.String(500), nullable=True), schema=S)
    op.execute(f"UPDATE {S}.activity_images ai SET s3_key = f.key FROM {S}.files f WHERE f.id = ai.file_id")
    op.alter_column('activity_images', 's3_key', nullable=False, schema=S)
    op.drop_constraint('activity_images_file_id_fkey', 'activity_images', schema=S, type_='foreignkey')
    op.drop_column('activity_images', 'file_id', schema=S)

    # children.parent_id -> child_parents
    op.create_table(
        'child_parents',
        sa.Column('child_id', sa.UUID(), nullable=False),
        sa.Column('parent_id', sa.UUID(), nullable=False),
        sa.Column('relationship', sa.String(100), nullable=True),
        sa.Column('is_primary', sa.Boolean(), nullable=False),
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['child_id'], [f'{S}.children.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['parent_id'], [f'{S}.users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('child_id', 'parent_id'),
        schema=S,
    )
    op.create_index(f'ix_{S}_child_parents_child_id', 'child_parents', ['child_id'], schema=S)
    op.create_index(f'ix_{S}_child_parents_parent_id', 'child_parents', ['parent_id'], schema=S)
    op.execute(f"""
        INSERT INTO {S}.child_parents (id, created_at, updated_at, child_id, parent_id, is_primary)
        SELECT gen_random_uuid(), now(), now(), id, parent_id, true FROM {S}.children WHERE parent_id IS NOT NULL
    """)
    op.drop_index(f'ix_{S}_children_parent_id', table_name='children', schema=S)
    op.drop_constraint('children_parent_id_fkey', 'children', schema=S, type_='foreignkey')
    op.drop_column('children', 'parent_id', schema=S)

    op.drop_index('uq_messages_attachment_live', table_name='messages', schema=S)
    for table, col, fk in [('users', 'avatar_file_id', 'fk_users_avatar_file_id'),
                           ('estancias', 'logo_file_id', 'fk_estancias_logo_file_id'),
                           ('children', 'photo_file_id', 'fk_children_photo_file_id'),
                           ('messages', 'attachment_file_id', 'fk_messages_attachment_file_id')]:
        op.drop_constraint(fk, table, schema=S, type_='foreignkey')
        op.drop_column(table, col, schema=S)

    op.drop_table('audit_log', schema=S)
    op.drop_table('child_documents', schema=S)
    op.drop_table('files', schema=S)
    for t in EXISTING_SOFT:
        op.drop_column(t, 'status', schema=S)

    op.execute(f"DROP FUNCTION {S}.forbid_delete()")
    op.execute(f"DROP FUNCTION {S}.forbid_audit_change()")
    bind = op.get_bind()
    for name, values in ENUMS.items():
        postgresql.ENUM(*values, name=name, schema=S).drop(bind, checkfirst=True)
