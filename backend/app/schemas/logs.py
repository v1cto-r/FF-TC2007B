"""Daily logs (with check-in / stay / check-out) and activities."""
import uuid
from datetime import date, time

from pydantic import BaseModel, Field

from app.database.models import Amount
from app.schemas.common import Name, OptName, OptText, RecordOut


class CheckInIn(BaseModel):
    check_in_time: time
    dropped_off_by_name: Name
    received_by_staff_name: Name
    is_sick: bool
    medication_left_with_prescription: bool | None = None
    arrives_with_symptoms: bool
    symptoms_description: OptText = None
    arrived_clean: bool
    brought_complete_backpack: bool
    good_physical_condition: bool
    injury_body_part: OptName = None
    injury_description: OptText = None
    parents_notes: OptText = None


class StayIn(BaseModel):
    feeding_breakfast: Amount
    feeding_snack: Amount
    feeding_lunch: Amount
    feeding_afternoon_snack: Amount | None = None
    feeding_notes: OptText = None
    slept: bool
    nap_minutes: int | None = Field(default=None, ge=0, le=1440)
    rest_comments: OptText = None
    toileting_notice: bool | None = None
    pee_count: int = Field(default=0, ge=0, le=100)
    poop_count: int = Field(default=0, ge=0, le=100)
    toileting_notes: OptText = None
    mood: Name
    cried: bool
    fought: bool
    participated: bool
    mood_notes: OptText = None
    had_accident: bool
    accident_description: OptText = None
    accident_report_folio: str | None = Field(default=None, max_length=100)
    health_issue: bool
    health_issue_description: OptText = None
    care_provided: OptText = None
    general_health_notes: OptText = None


class CheckOutIn(BaseModel):
    check_out_time: time
    released_in_good_physical_condition: bool
    injury_body_part: OptName = None
    injury_description: OptText = None
    released_clean: bool
    belongings_complete: bool
    notes: OptText = None
    picked_up_by_name: Name
    released_by_staff_name: Name
    # A pickup by someone the parent authorized; then id_photo_file_id (PICKUP_ID_PHOTO) is required
    pickup_authorization_id: uuid.UUID | None = None
    id_photo_file_id: uuid.UUID | None = None


class CheckInOut(CheckInIn, RecordOut):
    daily_log_id: uuid.UUID


class StayOut(StayIn, RecordOut):
    daily_log_id: uuid.UUID


class CheckOutOut(CheckOutIn, RecordOut):
    daily_log_id: uuid.UUID


class ActivityImageOut(RecordOut):
    file_id: uuid.UUID
    position: int


class ActivityIn(BaseModel):
    daily_log_id: uuid.UUID
    name: Name
    image_file_ids: list[uuid.UUID] = Field(default_factory=list, max_length=20)  # order = position


class ActivityOut(RecordOut):
    daily_log_id: uuid.UUID
    name: str
    images: list[ActivityImageOut]


class DailyLogIn(BaseModel):
    """A part that is left out is not touched, a part that is sent is created or replaced, and a part sent
    as an explicit null is removed (a check-out that was undone)."""
    child_id: uuid.UUID
    date: date
    caregiver_name: Name
    check_in: CheckInIn | None = None
    stay: StayIn | None = None
    check_out: CheckOutIn | None = None


class DailyLogOut(RecordOut):
    child_id: uuid.UUID
    date: date
    caregiver_id: uuid.UUID | None
    caregiver_name: str
    check_in: CheckInOut | None
    stay: StayOut | None
    check_out: CheckOutOut | None
    activities: list[ActivityOut]
