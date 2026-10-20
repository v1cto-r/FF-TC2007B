# Authentication and permissions

## How it works

Two JWTs, signed with two different symmetric keys (HS256):

| | Session token | Refresh token |
|---|---|---|
| Key | `SESSION_SECRET_KEY` | `REFRESH_SECRET_KEY` |
| Lifetime | 5 minutes | 7 days |
| Sent as | `Authorization: Bearer ...` on every request | JSON body of `/auth/refresh` and `/auth/logout` only |
| Stored in DB | No | Yes (SHA-256 hash, table `refresh_tokens`) |
| Contains | `sub`, `email`, `full_name`, `role`, `estancia_id`, `pending`, `typ`, `jti`, `iat`, `exp` | `sub`, `typ`, `jti`, `iat`, `exp` |

```
login -> session (5 min) + refresh (7 d, saved hashed in DB)

 ...requests with session...
 
session expired ──► POST /auth/refresh {refresh_token}
                    old refresh revoked, new session + new refresh issued
```

### Endpoints

| Endpoint | Who | What                                                                                                     |
|---|---|----------------------------------------------------------------------------------------------------------|
| `POST /auth/login` | anyone | OAuth2 password form (`username` = email). Returns `access_token`, `refresh_token`, `pending`.           |
| `POST /auth/refresh` | anyone with a refresh token | Rotates the refresh token, returns a new pair.                                                           |
| `POST /auth/logout` | anyone with a refresh token | Revokes that refresh token.                                                                              |
| `GET /auth/me` | logged in | Returns the session claims.                                                                              |
| `POST /auth/change-password` | logged in (even if pending) | Needs the current password. Ends the first-time setup, signs out all other sessions, returns a new pair. |
| `POST /auth/revoke` | `session:revoke` | Signs a user out (`{"user_id": ...}`).                                                                   |
| `POST /users` and the rest of `/users` | `user:*` | Create, list, edit, reset password, archive, delete. |

### Protecting an endpoint

```python
from app.auth.dependency import require_action, get_claims

@router.get("/children")
def list_children(claims = Depends(require_action("child:read"))):
    ...                      # claims.sub, claims.role, claims.estancia_id ...

@router.get("/whoami")
def whoami(claims = Depends(get_claims)):   # any valid session token, no action check
    ...
```

`require_access(action)` does the same check and also returns an `Access` (below), which is what every data endpoint uses.
`authenticated()` only needs a signed-in, non-pending user (editing your own profile). Both record the user as the author
of every change made in the request, for the [audit log](database.md#audit-log).

### Who can reach which rows (scopes)

`Access` (`app/auth/access.py`) is loaded from the database on every request: the ids of the estancias, salones and children
the user may see (ORG sees everything). Deleted rows are left out, archived ones stay. A role change takes effect on the
next request here, unlike the 5-minute token. Lists filter with these ids and a single record outside them is a `404`,
never a `403`, so the API doesn't confirm that it exists. See [database.md](database.md#access-model).

`get_claims` checks only the signature, the expiry and the token type, then returns the claims. It does not touch the database.
`require_action` does that, then checks the role has the action.

### Roles, actions and attributes (RBAC + ABAC)

- Roles are the fixed `UserRole` enum: `ORG`, `ADMIN`, `DOCENTE`, `PADRE`.
- Actions are named `resource:verb`. The verbs are `read`, `write` (every `PUT`, archive and unarchive), `delete`, and `create` (only for users). `child:*` grants every verb of a resource and `*` grants everything.
- The role -> actions mapping is the table `role_actions`. The starter fills it from `DEFAULT_ROLE_ACTIONS` (`auth/permissions.py`) **only when the table is empty**, and migration `0002` adds the new actions to existing databases. Changing the defaults in code later does not change a running database; edit the rows (or add a migration).
- The action is the coarse gate. Rules about *which row*, and a few about *which role*, are plain code inside the routes, so a mistake in `role_actions` can't hand out powers that are meant to be fixed.

Default grants:

| Resource | ORG | ADMIN | DOCENTE | PADRE |
|---|---|---|---|---|
| estancia | all | read, write (own) | read | read |
| user | all | all (own estancia) | - | - |
| session:revoke | yes | yes (own estancia) | - | - |
| salon | read | all | read | read |
| child | read | all | read | read |
| daily_log | read | read, write (correct) | all | read |
| notice | read | all | all (own salones) | read |
| message | read, write | read, write, delete | read, write | read, write, delete |
| file | all | all | all | all |
| document | read | read | read | all |

Rules in code that no grant can change:

- `ORG` creates `ADMIN`s and estancias; `ADMIN` creates `DOCENTE`s and `PADRE`s, who join the admin's estancia. An `ADMIN` only manages the people of its own estancia (`can_manage_user`).
- Only the `PADRE` of a child sets the child's photo and manages their documents.
- Only the people of a chat send in it ([chat.md](chat.md): a child's PADRE / DOCENTE / ADMIN, a DOCENTE and their ADMIN, an ADMIN and ORG). ORG reads every chat but only writes in ADMIN ones. Sending goes over the [chat socket](chat.md), which authenticates with the session token and re-checks access on every message; nobody edits messages.
- Only the child's `PADRE` or the estancia's `ADMIN` deletes messages, and only in a child's chat (any message in it). DOCENTE ↔ ADMIN and ADMIN ↔ ORG messages are never deleted.
- Only a `DOCENTE` uploads activity photos; only an `ADMIN` uploads the estancia logo and posts to a whole estancia.
- Archiving or deleting a user blocks their login and refresh immediately and revokes their refresh tokens. Their current session token still works until it expires (5 minutes, the stateless-token tradeoff below).

### Creating users and first login

`POST /users` creates the user, a row in `user_profiles` with `pending = true`, and returns a **temporary password once**: three random words from `auth/wordlist.txt` joined by dashes (e.g. `pero-bien-para`), picked with `secrets.choice`. The list is loaded into memory when the API starts (`load_wordlist()` in the lifespan). It is easy to read out loud, but weak (about 30 bits with 1185 words), which is acceptable only because it must be changed at first login and login is rate limited. The user's session token then has `pending: true`, and everything except `/auth/me` and `/auth/change-password` answers 403. After `/auth/change-password` the profile is no longer pending and a normal token pair comes back. The app can use the `pending` field of the login response to show its setup screen.

### Default ORG user

On start, if the database has no `ORG` user, the starter creates one from `DEFAULT_ORG_EMAIL` / `DEFAULT_ORG_PASSWORD` (default `admin` / `admin`) with a pending profile. The first login only reaches `/auth/change-password`; after that it is a normal ORG account and creates the `ADMIN`s. Set your own values in `.env` before the first deploy, since the default is public in this repo until it is changed.

### Refresh token rotation and reuse detection

Every refresh revokes the used token and issues a new one in the same *family* (one family per login). If an already revoked token is presented, someone else has a copy, so the whole family is revoked and both parties must log in again.

### Rate limiting

Counters live in Postgres, table `rate_limits` (key, hits, expires_at), used like Redis: one atomic upsert per request, fixed windows. The starter makes it `UNLOGGED` (fast, wiped on a database crash, which only resets the counters).

- **Every endpoint** (including the chat socket's handshake) has a default of 120 requests/minute (`dependencies=` on the `FastAPI` app in `main.py`).
- A route can add its own limit: `dependencies=[Depends(rate_limit("name", 20, 60))]`.
- The bucket is the **user id** from a valid session token, otherwise the **client IP** (`CF-Connecting-IP`, since the API is behind Cloudflare).
- `/auth/login` also limits per **email + IP** pair (5/minute), so someone attacking one account from a shared network only blocks themselves.
- Over the limit: `429` with `Retry-After`.
- Chat messages sent over the socket: 60/minute per user, answered with an `error` frame (`status` 429).

## Decisions and why

- **Two keys.** A leaked session key can't forge refresh tokens, and the typ claim is checked too.
- **Stateless session token.** No DB read to authenticate, and the route gets the user's data from the token. The cost: a role change or deactivation applies after at most 5 minutes (when the session expires and `/auth/refresh` looks at the user again), and `/auth/revoke` only blocks refreshing. `require_action` still reads `role_actions` on each call, so changes to a role's actions apply immediately.
- **Refresh tokens in the DB, hashed.** Needed to revoke them; a database leak does not give usable tokens.
- **Bearer header + JSON body**, no cookies: the client is the Android app, so there is no CSRF surface.
- **Roles stay an enum, actions are data.** Roles rarely change, but who can do what should be editable without a deploy.
- **Postgres instead of Redis for counters.** One less service to run on the single EC2 instance. No need for a good p99 latency. 
