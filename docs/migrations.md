# Database migrations (Alembic)

Revisions live in `backend/migrations/versions/`; the API runs `alembic upgrade head` on start.

| Revision | What |
|---|---|
| `0001_baseline` | The schema exactly as `create_all` made it before Alembic. |
| `0002_soft_delete_audit_files` | `status` on the domain tables and the delete-blocking triggers, partial unique indexes, `ON DELETE RESTRICT`, `audit_log`, `files` and `child_documents`, `children.parent_id` replacing `child_parents`, new `role_actions`. Moves existing data (parents, photo keys) across. |
| `0004_child_birth_data` | `children.birth_date`, `children.allergies` and `children.profile_completed_at`. Existing children keep `birth_date` null. |
| `0005_peer_chats` | `messages.peer_id` (DOCENTE ↔ ADMIN and ADMIN ↔ ORG chats) with `messages.child_id` now nullable, `files.peer_id` for their photos, and `message:write` / `message:delete` for ADMIN, `message:write` for ORG (only on databases whose `role_actions` is already seeded). Downgrading fails while peer chat messages exist. |

## Databases created before Alembic

A database that has tables but no `alembic_version` was made by the old `create_all`. On start it is **stamped** at `0001`
(nothing is recreated) and then upgraded, so existing data survives. This path is covered by
`test_pre_alembic_database_is_stamped_then_upgraded`.

## Changing the schema

1. Edit the models in `backend/app/database/models.py`.
2. Generate a draft and **read it**; autogenerate misses things (enum changes, data moves, triggers, partial indexes' predicates):

   ```bash
   cd backend
   export POSTGRES_USER=ff POSTGRES_PASSWORD=ff POSTGRES_DB=ff_dev DB_HOST=localhost DB_PORT=5432 S3_ACCESS_KEY=x S3_SECRET_KEY=x SESSION_SECRET_KEY=a REFRESH_SECRET_KEY=b
   alembic upgrade head                       # the dev database must be at head first
   alembic revision --autogenerate -m "what changed" --rev-id 0003
   ```
3. Write the data migration in the same revision when rows have to move (see `0002` for `child_parents` -> `parent_id`).
4. Write a real `downgrade()`.
5. Run the tests: `test_models_and_migrations_agree` fails if the models and the migrations disagree, and
   `test_migrations.py` is the place to add a test that your data move keeps the data.
6. If you add a table that holds records, add it to the delete-blocking triggers and to `ALL_SOFT` in the migration, give
   it `Record` as base class (`status`, audited), and add it to `SOFT_TABLES` in `test_lifecycle_audit.py`.

Notes:

* Alembic can't see foreign keys correctly here (the models say `public.users`, the database reports `users`), so
  `env.py` leaves FKs out of the drift check. Their behaviour is covered by tests (`ON DELETE RESTRICT`, the trigger).
* New actions for `role_actions` go into `DEFAULT_ROLE_ACTIONS` **and** into a migration that inserts them for existing
  databases (`test_code_and_migration_grant_the_same_actions` keeps the two lists equal for `0002`).
* Never edit a revision that has been applied anywhere; add a new one.

## Commands

```bash
alembic current            # which revision the database is at
alembic upgrade head       # apply everything
alembic downgrade 0001     # step back (test it on a copy first: 0002 downgrade drops the audit log)
alembic check              # fails if the models have changes without a migration
```
