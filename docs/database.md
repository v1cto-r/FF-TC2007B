# Sonrisas database

```mermaid
erDiagram
  USERS {
    uuid id PK
    string email UK
    string password_hash
    string full_name
    enum role "ORG|ADMIN|DOCENTE|PADRE"
    uuid estancia_id FK "only DOCENTE"
    bool is_active
  }
  ESTANCIAS {
    uuid id PK
    string name
    string address
    uuid admin_user_id FK,UK "1 admin per estancia"
  }
  SALONES {
    uuid id PK
    uuid estancia_id FK
    string name "UK with estancia_id"
    enum group_type "L|M|P"
    uuid docente_id FK
  }
  CHILDREN {
    uuid id PK
    uuid estancia_id FK
    uuid salon_id FK
    string full_name
  }
  CHILD_PARENTS {
    uuid id PK
    uuid child_id FK "UK with parent_id"
    uuid parent_id FK
    string relationship
    bool is_primary
  }
  DAILY_LOGS {
    uuid id PK
    uuid child_id FK "UK with date"
    date date
    uuid caregiver_id FK
    string caregiver_name
  }
  CHECK_INS {
    uuid id PK
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
  }
  STAYS {
    uuid id PK
    uuid daily_log_id FK,UK
    enum feeding_breakfast "NONE|SOME|ALL|ASKED_FOR_MORE"
    enum feeding_snack
    enum feeding_lunch
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
  }
  CHECK_OUTS {
    uuid id PK
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
  }
  ACTIVITY_RECORDS {
    uuid id PK
    uuid daily_log_id FK
    string name
  }
  ACTIVITY_IMAGES {
    uuid id PK
    uuid activity_record_id FK
    int position
    string s3_key
  }
  NOTICES {
    uuid id PK
    uuid estancia_id FK
    uuid salon_id FK "null = whole estancia"
    uuid author_id FK
    string name
    text description
    date date
  }
  MESSAGES {
    uuid id PK
    uuid child_id FK "Room chatId"
    uuid author_id FK
    text content
    timestamptz sent_at
    bool is_aviso
  }

  USERS ||--o| ESTANCIAS : "admin_user_id"
  ESTANCIAS |o--o{ USERS : "users.estancia_id (docentes)"
  ESTANCIAS ||--o{ SALONES : "salones.estancia_id"
  ESTANCIAS ||--o{ CHILDREN : "children.estancia_id"
  ESTANCIAS ||--o{ NOTICES : "notices.estancia_id"
  USERS |o--o{ SALONES : "salones.docente_id"
  SALONES |o--o{ CHILDREN : "children.salon_id"
  SALONES |o--o{ NOTICES : "notices.salon_id"
  CHILDREN ||--o{ CHILD_PARENTS : "child_parents.child_id"
  USERS ||--o{ CHILD_PARENTS : "child_parents.parent_id"
  CHILDREN ||--o{ DAILY_LOGS : "daily_logs.child_id"
  CHILDREN ||--o{ MESSAGES : "messages.child_id"
  USERS |o--o{ DAILY_LOGS : "daily_logs.caregiver_id"
  USERS |o--o{ NOTICES : "notices.author_id"
  USERS |o--o{ MESSAGES : "messages.author_id"
  DAILY_LOGS ||--o| CHECK_INS : "check_ins.daily_log_id"
  DAILY_LOGS ||--o| STAYS : "stays.daily_log_id"
  DAILY_LOGS ||--o| CHECK_OUTS : "check_outs.daily_log_id"
  DAILY_LOGS ||--o{ ACTIVITY_RECORDS : "activity_records.daily_log_id"
  ACTIVITY_RECORDS ||--o{ ACTIVITY_IMAGES : "activity_images.activity_record_id"
```

## Access model
- **ORG**: Reads across all estancias, just overall states.
- **ADMIN**: the user in `estancias.admin_user_id`. Manages that estancia.
- **DOCENTE**: `users.estancia_id`, and assigned to salones via `salones.docente_id`.
- **PADRE**: no direct estancia or salon. Reached through `child_parents` to `children`, then `estancia_id` / `salon_id` by join.

