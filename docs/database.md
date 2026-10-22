# Sonrisas database

PostgreSQL. Models: `backend/app/database/models.py`. Schema changes: [migrations.md](migrations.md).

```mermaid
erDiagram
  ACTIVITY_IMAGES {
    uuid activity_record_id FK
    uuid file_id FK,UK
    int position
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  ACTIVITY_RECORDS {
    uuid daily_log_id FK
    string name
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  AUDIT_LOG {
    uuid id PK
    timestamptz created_at
    uuid actor_id FK
    enum action "CREATE|UPDATE|DELETE|ARCHIVE|UNARCHIVE"
    string resource_type
    uuid resource_id
    jsonb changes
  }
  CHECK_INS {
    uuid daily_log_id FK,UK
    time check_in_time
    string dropped_off_by_name
    string received_by_staff_name
    bool is_sick
    bool medication_left_with_prescription
    bool arrives_with_symptoms
    text symptoms_description
    bool arrived_clean
    bool brought_complete_backpack
    bool good_physical_condition
    string injury_body_part
    text injury_description
    text parents_notes
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  CHECK_OUTS {
    uuid daily_log_id FK,UK
    time check_out_time
    bool released_in_good_physical_condition
    string injury_body_part
    text injury_description
    bool released_clean
    bool belongings_complete
    text notes
    string picked_up_by_name
    string released_by_staff_name
    uuid pickup_authorization_id FK
    uuid id_photo_file_id FK
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  CHILD_DOCUMENTS {
    uuid child_id FK
    uuid file_id FK,UK
    string title
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  CHILDREN {
    uuid estancia_id FK
    uuid salon_id FK
    uuid parent_id FK
    string full_name
    date birth_date "null only for children enrolled before it existed"
    string allergies "comma separated, max 1000 chars"
    timestamptz profile_completed_at "set when the parent sends the profile"
    uuid photo_file_id FK
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  DAILY_LOGS {
    uuid child_id FK
    date date
    uuid caregiver_id FK
    string caregiver_name
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  ESTANCIAS {
    string name
    string address
    uuid admin_user_id FK,UK
    uuid logo_file_id FK
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  FILES {
    string bucket
    string key UK
    enum purpose "ACTIVITY_IMAGE|MESSAGE_ATTACHMENT|CHILD_PHOTO|USER_AVATAR|ESTANCIA_LOGO|CHILD_DOCUMENT|PICKUP_ID_PHOTO"
    string kind
    string content_type
    int size
    string original_name
    enum upload_state "PENDING|READY"
    uuid uploaded_by FK
    uuid estancia_id FK
    uuid child_id FK
    uuid peer_id FK
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  MESSAGES {
    uuid child_id FK
    uuid peer_id FK
    uuid author_id FK
    text content
    timestamptz sent_at
    bool is_aviso
    uuid attachment_file_id FK,UK
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  NOTICES {
    uuid estancia_id FK
    uuid salon_id FK
    uuid author_id FK
    string name
    text description
    date date
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  RATE_LIMITS {
    string key PK
    int hits
    float expires_at
  }
  REFRESH_TOKENS {
    uuid user_id FK
    uuid family_id
    string token_hash UK
    timestamptz expires_at
    timestamptz revoked_at
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  PICKUP_AUTHORIZATIONS {
    uuid child_id FK
    uuid created_by FK
    enum kind "THIS_WEEK|RECURRING"
    date start_date
    int weekdays
    string authorized_person_name
    string relationship
    string phone
    string identification_type
    text notes
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  ROLE_ACTIONS {
    enum role "ORG|ADMIN|DOCENTE|PADRE"
    string action
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  SALONES {
    uuid estancia_id FK
    string name
    enum group_type "L|M|P"
    uuid docente_id FK
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  STAYS {
    uuid daily_log_id FK,UK
    enum feeding_breakfast "NONE|SOME|ALL|ASKED_FOR_MORE"
    enum feeding_snack "NONE|SOME|ALL|ASKED_FOR_MORE"
    enum feeding_lunch "NONE|SOME|ALL|ASKED_FOR_MORE"
    enum feeding_afternoon_snack "NONE|SOME|ALL|ASKED_FOR_MORE"
    text feeding_notes
    bool slept
    int nap_minutes
    text rest_comments
    bool toileting_notice
    int pee_count
    int poop_count
    text toileting_notes
    string mood
    bool cried
    bool fought
    bool participated
    text mood_notes
    bool had_accident
    text accident_description
    string accident_report_folio
    bool health_issue
    text health_issue_description
    text care_provided
    text general_health_notes
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }
  USER_PROFILES {
    uuid user_id PK,FK
    bool pending
  }
  USERS {
    string email UK
    string password_hash
    string full_name
    enum role "ORG|ADMIN|DOCENTE|PADRE"
    uuid estancia_id FK
    bool is_active
    uuid avatar_file_id FK
    enum status "ARCHIVED|DELETED"
    uuid id PK
    timestamptz created_at
    timestamptz updated_at
  }

  ACTIVITY_RECORDS ||--o{ ACTIVITY_IMAGES : "activity_record_id"
  CHILDREN |o--o{ FILES : "child_id"
  CHILDREN ||--o{ CHILD_DOCUMENTS : "child_id"
  CHILDREN ||--o{ DAILY_LOGS : "child_id"
  CHILDREN ||--o{ MESSAGES : "child_id"
  CHILDREN ||--o{ PICKUP_AUTHORIZATIONS : "child_id"
  PICKUP_AUTHORIZATIONS |o--o{ CHECK_OUTS : "pickup_authorization_id"
  FILES |o--o| CHECK_OUTS : "id_photo_file_id"
  DAILY_LOGS ||--o{ ACTIVITY_RECORDS : "daily_log_id"
  DAILY_LOGS ||--o| CHECK_INS : "daily_log_id"
  DAILY_LOGS ||--o| CHECK_OUTS : "daily_log_id"
  DAILY_LOGS ||--o| STAYS : "daily_log_id"
  ESTANCIAS |o--o{ FILES : "estancia_id"
  ESTANCIAS |o--o{ USERS : "estancia_id"
  ESTANCIAS ||--o{ CHILDREN : "estancia_id"
  ESTANCIAS ||--o{ NOTICES : "estancia_id"
  ESTANCIAS ||--o{ SALONES : "estancia_id"
  FILES |o--o{ CHILDREN : "photo_file_id"
  FILES |o--o{ ESTANCIAS : "logo_file_id"
  FILES |o--o{ USERS : "avatar_file_id"
  FILES |o--o| MESSAGES : "attachment_file_id"
  FILES ||--o| ACTIVITY_IMAGES : "file_id"
  FILES ||--o| CHILD_DOCUMENTS : "file_id"
  SALONES |o--o{ CHILDREN : "salon_id"
  SALONES |o--o{ NOTICES : "salon_id"
  USERS |o--o{ AUDIT_LOG : "actor_id"
  USERS |o--o{ CHILDREN : "parent_id"
  USERS |o--o{ DAILY_LOGS : "caregiver_id"
  USERS |o--o{ MESSAGES : "author_id"
  USERS |o--o{ MESSAGES : "peer_id"
  USERS |o--o{ FILES : "peer_id"
  USERS |o--o{ NOTICES : "author_id"
  USERS |o--o{ SALONES : "docente_id"
  USERS ||--o{ FILES : "uploaded_by"
  USERS ||--o{ REFRESH_TOKENS : "user_id"
  USERS ||--o{ USER_PROFILES : "user_id"
  USERS ||--o| ESTANCIAS : "admin_user_id"
```

Notes on the diagram: `rate_limits`, `refresh_tokens`, `device_tokens` (FCM push registrations), `role_actions` and `user_profiles` are bookkeeping for auth.
`ACTIVITY_IMAGES`, `MESSAGES`, `USERS`, `ESTANCIAS` and `CHILDREN` point at `FILES` for their images; `FILES` points back
at the user, estancia and child it belongs to (those foreign keys are added after the tables exist, because the
references go in a circle).

## Pickup authorizations

Who besides the parent may pick a child up (`app/pickups.py`). `weekdays` is a bitmask (Monday = 1 ... Sunday = 64):

- `THIS_WEEK`: those days of one week only; `start_date` is its Monday. Days already past can't be added. It is
  no longer listed once the week is over.
- `RECURRING`: those days of every week from `start_date` (the day it was created) until the parent cancels it.

At most one applies per child and day: a `THIS_WEEK` one wins over a `RECURRING` one on its days, and two of the
same kind that share a day are refused (409, or `?replace=true` cancels the old ones). "Today" and "this week" are
in `LOCAL_TIMEZONE` (default `America/Mexico_City`). A check-out to an authorized person stores
`pickup_authorization_id` and requires `id_photo_file_id` (a `PICKUP_ID_PHOTO` the DOCENTE takes).

## Nothing is ever deleted

Every domain table (everything except the auth bookkeeping above) has a nullable `status` column:

| `status` | Meaning | Visible to | Editable |
|---|---|---|---|
| `NULL` | active | everyone with access | yes |
| `ARCHIVED` | kept and listed, e.g. a child who graduated | everyone with access | no (`409`) until unarchived |
| `DELETED` | removed | **ORG only** (`?include_deleted=true`) | no |

* **The API never issues `DELETE`.** `DELETE /x/{id}` sets `status = 'DELETED'`.
* **The database enforces it.** A `BEFORE DELETE` row trigger and a `BEFORE TRUNCATE` trigger (`forbid_delete()`) on
  every one of those tables raise an error, so not even a bug, a script or a hand-typed `DELETE` can destroy a record.
  Foreign keys are `ON DELETE RESTRICT` (or `SET NULL` where a missing link is fine) for the same reason.
* **A parent can't be deleted while it has *active* dependents** (`409`): a salon with children, an estancia with
  salones, a child with daily logs, messages or documents, a user who still runs an estancia / teaches a salon /
  has children. `ARCHIVED` and `DELETED` dependents don't block. Parts that belong to a record are deleted with it:
  a daily log takes its check-in, stay, check-out, activities and photos along.
* **Unique values ignore deleted rows.** `users.email`, a salon's name inside its estancia, a child's log for a day,
  an estancia's admin and the 1:1 stage tables use partial unique indexes
  (`... WHERE status IS DISTINCT FROM 'DELETED'`), so a deleted salon's name or a deleted user's email can be used again.
  `ARCHIVED` rows still hold their values.
* **Files are kept too.** A deleted file only changes `status`; the object stays in RustFS. Only ORG can still get a
  download URL for it.

## Audit log

`audit_log` is append-only (a trigger rejects `UPDATE`, `DELETE` and `TRUNCATE`). One row per change of any record:

| column | |
|---|---|
| `actor_id` | the signed-in user who made the change; `NULL` = the system (startup seeding, expiry of unconfirmed uploads) |
| `action` | `CREATE`, `UPDATE`, `DELETE` (soft), `ARCHIVE`, `UNARCHIVE` |
| `resource_type`, `resource_id` | the table and the row |
| `changes` | on create the values set; on update `{"field": [old, new]}` for the fields that changed |
| `created_at` | when |

It is written by a SQLAlchemy `before_flush` hook (`app/database/audit.py`) in the **same transaction** as the change, so
either both are saved or neither is. `password_hash` is stored as `***`. Only changes made through the ORM are seen,
so records must never be changed with a bulk `UPDATE` statement. **The log holds copies of what was written** (chat messages, health notes of a stay...), so it is as sensitive as the tables themselves: don't expose it to anyone who couldn't read the originals. There is no endpoint for reading the log yet; it is
queried in SQL. Each change is also written to the API's stdout as a JSON line, without the values
(see [observability.md](observability.md)).

## Access model

Who can *reach* which rows (`app/auth/access.py`, resolved from the database on every request):

| Role | Reaches |
|---|---|
| **ORG** | everything, including deleted rows. Creates estancias and their admins. |
| **ADMIN** | its estancia (`estancias.admin_user_id`) and every salon, child and person in it. |
| **DOCENTE** | its estancia (`users.estancia_id`) and the salones where `salones.docente_id` is them, with those salones' children. |
| **PADRE** | its children (`children.parent_id`), their salones, and its estancia (`users.estancia_id`). |

A child has exactly one parent account (`children.parent_id`); a parent can have several children. DOCENTEs and PADREs
get `users.estancia_id` from the admin who created them. Every `/children` response carries `parent_name` (read-only, the parent's
`full_name`), so a DOCENTE sees who the parent is without `user:read`. `DELETED` rows drop out of these sets; `ARCHIVED` ones stay.

What each role may *do* with what it reaches is the `role_actions` table (see [auth.md](auth.md)) plus a few rules
written in code (only the parent or the admin deletes chat messages, only the docente of the salon uploads activity photos...).
