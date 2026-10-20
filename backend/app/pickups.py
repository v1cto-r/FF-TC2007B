"""Which pickup authorization applies on a day, and which ones would overlap.

Both kinds store the weekdays they cover as a bitmask (Monday = 1 ... Sunday = 64):

* THIS_WEEK: those days of one week only (`start_date` is that week's Monday).
* RECURRING: those days of every week from `start_date` on, until the parent cancels it.

On a given day a THIS_WEEK authorization wins over a RECURRING one ("this Friday my sister picks up
instead"), so the two kinds never conflict. Two of the same kind that share a day do. The app
(PickupAuthorization.kt) implements the same rules.
"""
from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo

from app.config import settings
from app.database.models import PickupAuthorization, PickupKind


def local_today() -> date:
    """Today where the estancias are, so a parent and the server agree on the day near midnight."""
    return datetime.now(ZoneInfo(settings.local_timezone)).date()


def week_start(day: date) -> date:
    return day - timedelta(days=day.weekday())


def to_mask(weekdays: list[int]) -> int:
    """ISO weekdays (1 = Monday ... 7 = Sunday) -> bitmask."""
    mask = 0
    for d in weekdays:
        mask |= 1 << (d - 1)
    return mask


def to_weekdays(mask: int) -> list[int]:
    return [d for d in range(1, 8) if mask & (1 << (d - 1))]


def covers_weekday(mask: int, day: date) -> bool:
    return bool(mask & (1 << day.weekday()))


def applies_on(p: PickupAuthorization, day: date) -> bool:
    if not covers_weekday(p.weekdays, day):
        return False
    if p.kind == PickupKind.THIS_WEEK:
        return p.start_date <= day < p.start_date + timedelta(days=7)
    return p.start_date <= day


def resolve(authorizations: list[PickupAuthorization], day: date) -> PickupAuthorization | None:
    applying = [p for p in authorizations if applies_on(p, day)]
    return next((p for p in applying if p.kind == PickupKind.THIS_WEEK), None) or next(iter(applying), None)


def conflicts(kind: PickupKind, start_date: date, mask: int, other: PickupAuthorization) -> bool:
    if kind != other.kind or not (mask & other.weekdays):
        return False
    return kind == PickupKind.RECURRING or start_date == other.start_date
