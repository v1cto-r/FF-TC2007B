# Tests and CI

Tests are in `backend/test/` and run against Postgres and RustFS in Docker.

## Run them

To run the tests, run directly from docker. Builds from a test dockerfile. It also spins up the rustfs and postgres instance.

```bash
docker compose --profile test run --rm --build ff_api_test      # exit code 0 = all green
docker compose --profile test rm -sf ff_test_postgres ff_test_rustfs ff_api_test   # clean up afterwards
```

The `test` profile services (`ff_test_postgres`, `ff_test_rustfs`, `ff_api_test`) use their own credentials and no volumes, so
they never touch the real database or storage.

> **Don't clean up with `docker compose down -v`.** It removes every named volume of the project, including the real
> `postgres_data` and `rustfs_data`.


## How the suite is built

* `conftest.py` builds the schema once with `alembic upgrade head` (so the migrations are what is tested), and runs every
  test inside a transaction that is rolled back. `world` is a ready-made scenario: two estancias, an admin each, docentes,
  parents and children, plus an ORG user; `auth(user)` returns that user's headers.
* S3 is faked (`FakeS3`) except in `test_rustfs_integration.py`; boto3 still produces real signed URLs.

| File | Covers |
|---|---|
| `test_migrations.py` | models == migrations, data kept by `0002`, downgrade, stamping of pre-Alembic databases |
| `test_lifecycle_audit.py` | delete triggers, append-only audit log, audit diffs and actors, partial unique indexes, blockers |
| `test_access.py` | which ids each role reaches |
| `test_org_api.py`, `test_users_api.py`, `test_logs_api.py`, `test_comms_api.py` | every endpoint (messages are sent through the socket, `test/chat_helpers.py`), per role, including who gets 403 / 404 / 409 |
| `test_chat_ws.py` | the chat socket: handshake and token expiry, routing to the right people, retries, photos, deletes, rate limit |
| `test_peer_chats.py` | DOCENTE ↔ ADMIN and ADMIN ↔ ORG chats: who reads / writes / receives, outsiders get 404, no deletes, `::Aviso` marker, photos |
| `test_devices_push.py` | device registration, who gets a push (and who doesn't), dead tokens, Firebase failures |
| `test_files_api.py`, `test_attachments_api.py` | the upload flow, type / size / content checks, per-purpose permissions, rate limits, expiry, downloads, photos, avatars, logos, documents |
| `test_defense_in_depth.py` | role rules in code hold even if `role_actions` grants everything |
| `test_storage_policy.py` | policy file, magic bytes, presigned URLs |
| `test_main.py` | every endpoint has a typed request and response model; errors use `Detail` |
| `test_rustfs_integration.py` | signed URLs against a real RustFS (`S3_INTEGRATION=1`) |

## CI

The whole pipeline is the one command above. For GitHub Actions, for example:

```yaml
name: backend
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: docker compose --profile test run --rm --build ff_api_test
      - if: always()
        run: docker compose --profile test rm -sf ff_test_postgres ff_test_rustfs ff_api_test
```
