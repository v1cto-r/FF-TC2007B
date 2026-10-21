import uuid
from collections import defaultdict
from datetime import date, datetime
from zoneinfo import ZoneInfo

from fastapi import APIRouter, Depends, HTTPException, Query, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from app import pickups
from app.auth.access import Access
from app.auth.dependency import require_access
from app.config import settings
from app.database import lifecycle
from app.database.connection import get_db
from app.database.models import (
    ActivityImage, ActivityRecord, CheckIn, CheckOut, Child, DailyLog, File, FilePurpose, PickupAuthorization,
    RecordStatus, Stay,
)
from app.reports import weekly
from app.schemas.common import Detail
from app.schemas.logs import (
    ActivityImageOut, ActivityIn, ActivityOut, CheckInOut, CheckOutIn, CheckOutOut, DailyLogIn, DailyLogOut, StayOut,
)
from app.storage.rules import load_attachable, release

router = APIRouter(tags=["daily-logs"])
ERRORS = {403: {"model": Detail}, 404: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail}}

PARTS = (("check_in", CheckIn), ("stay", Stay), ("check_out", CheckOut))


def _visible_log_or_404(db: Session, access: Access, log_id: uuid.UUID) -> DailyLog:
    log = lifecycle.get_or_404(db, DailyLog, log_id, access)
    if not access.can_see_child(log.child_id):
        raise HTTPException(404, "Daily log not found")
    return log


def _live_children_of(db: Session, model, column, ids, with_deleted: bool = False):
    stmt = select(model).where(column.in_(ids))
    return db.scalars(stmt if with_deleted else stmt.where(lifecycle.live(model))).all()


def _build_logs(db: Session, logs: list[DailyLog], with_deleted: bool = False) -> list[DailyLogOut]:
    """Logs with their stages and activities, loaded in a handful of queries."""
    ids = [log.id for log in logs]
    parts = {name: {p.daily_log_id: p for p in _live_children_of(db, model, model.daily_log_id, ids, with_deleted)}
             for name, model in PARTS}
    acts = _live_children_of(db, ActivityRecord, ActivityRecord.daily_log_id, ids, with_deleted)
    images = defaultdict(list)
    for img in db.scalars(select(ActivityImage).where(
            ActivityImage.activity_record_id.in_([a.id for a in acts]),
            *([] if with_deleted else [lifecycle.live(ActivityImage)])).order_by(ActivityImage.position)):
        images[img.activity_record_id].append(ActivityImageOut.model_validate(img))
    by_log = defaultdict(list)
    for a in acts:
        by_log[a.daily_log_id].append(_activity_out(a, images[a.id]))
    return [DailyLogOut(
        id=log.id, status=log.status, created_at=log.created_at, updated_at=log.updated_at,
        child_id=log.child_id, date=log.date, caregiver_id=log.caregiver_id, caregiver_name=log.caregiver_name,
        check_in=CheckInOut.model_validate(parts["check_in"][log.id]) if log.id in parts["check_in"] else None,
        stay=StayOut.model_validate(parts["stay"][log.id]) if log.id in parts["stay"] else None,
        check_out=CheckOutOut.model_validate(parts["check_out"][log.id]) if log.id in parts["check_out"] else None,
        activities=by_log[log.id]) for log in logs]


def _activity_out(a: ActivityRecord, images: list[ActivityImageOut]) -> ActivityOut:
    return ActivityOut(id=a.id, status=a.status, created_at=a.created_at, updated_at=a.updated_at,
                       daily_log_id=a.daily_log_id, name=a.name, images=images)


def _check_pickup(db: Session, log: DailyLog, data: CheckOutIn, row: CheckOut | None) -> None:
    """A release to an authorized person needs that person's authorization for this child and day, and a
    photo of their ID. Whether the authorization was cancelled since doesn't matter: an offline check-out can
    arrive late, and this records what happened."""
    if data.pickup_authorization_id is not None:
        p = db.get(PickupAuthorization, data.pickup_authorization_id)
        if p is None or p.child_id != log.child_id or not pickups.applies_on(p, log.date):
            raise HTTPException(422, "That pickup authorization doesn't cover this child on this day")
        if data.id_photo_file_id is None:
            raise HTTPException(422, "A pickup by an authorized person needs a photo of their ID")
    old = row.id_photo_file_id if row is not None else None
    if data.id_photo_file_id is not None and data.id_photo_file_id != old:
        load_attachable(db, data.id_photo_file_id, FilePurpose.PICKUP_ID_PHOTO, child_id=log.child_id)
        if db.scalar(select(CheckOut.id).where(CheckOut.id_photo_file_id == data.id_photo_file_id,
                                               lifecycle.live(CheckOut))):
            raise HTTPException(409, f"File {data.id_photo_file_id} is already used by another check-out")
    if old is not None and old != data.id_photo_file_id:
        release(db.get(File, old))


def _remove_part(db: Session, row) -> None:
    """A stage sent as an explicit null was undone in the app (a check-out cancelled)."""
    if isinstance(row, CheckOut) and row.id_photo_file_id is not None:
        release(db.get(File, row.id_photo_file_id))
    row.status = RecordStatus.DELETED


@router.get("/daily-logs", response_model=list[DailyLogOut])
def list_daily_logs(child_id: uuid.UUID | None = None, from_date: date | None = None, to_date: date | None = None,
                    include_deleted: bool = False, access: Access = Depends(require_access("daily_log:read")),
                    db: Session = Depends(get_db)):
    stmt = lifecycle.apply_visibility(select(DailyLog).order_by(DailyLog.date.desc()), DailyLog, access,
                                      include_deleted)
    if not access.is_org:
        stmt = stmt.where(DailyLog.child_id.in_(access.children))
    if child_id:
        stmt = stmt.where(DailyLog.child_id == child_id)
    if from_date:
        stmt = stmt.where(DailyLog.date >= from_date)
    if to_date:
        stmt = stmt.where(DailyLog.date <= to_date)
    return _build_logs(db, list(db.scalars(stmt)), with_deleted=include_deleted)


@router.get("/daily-logs/report", response_class=Response, responses={
    200: {"content": {"application/pdf": {}}, "description": "The weekly report as a PDF"}, **ERRORS})
def weekly_report(child_id: uuid.UUID, year: int = Query(ge=2000, le=2100), week: int = Query(ge=1, le=53),
                  access: Access = Depends(require_access("daily_log:read")), db: Session = Depends(get_db)):
    """The logs of one child for Monday to Friday of an ISO week, laid out like the daily filter log form."""
    if not access.can_see_child(child_id):
        raise HTTPException(404, "Child not found")
    child = lifecycle.get_or_404(db, Child, child_id, access)
    try:
        monday, friday = weekly.week_range(year, week)
    except ValueError:
        raise HTTPException(422, f"{year} has no week {week}")
    stmt = lifecycle.apply_visibility(select(DailyLog), DailyLog, access).where(
        DailyLog.child_id == child_id, DailyLog.date >= monday, DailyLog.date <= friday)
    logs = _build_logs(db, list(db.scalars(stmt)))
    info = weekly.ReportChild(full_name=child.full_name, estancia=child.estancia.name,
                              salon=child.salon.name if child.salon else None,
                              group_type=child.salon.group_type.value if child.salon else None)
    pdf = weekly.render_weekly_report(info, monday, {log.date: log for log in logs},
                                      generated_at=datetime.now(ZoneInfo(settings.local_timezone)))
    name = "".join(c if c.isascii() and c.isalnum() else "_" for c in child.full_name).strip("_") or "nino"
    return Response(pdf, media_type="application/pdf",
                    headers={"Content-Disposition": f'attachment; filename="reporte_{name}_{year}-W{week:02d}.pdf"'})


@router.get("/daily-logs/{log_id}", response_model=DailyLogOut, responses=ERRORS)
def get_daily_log(log_id: uuid.UUID, access: Access = Depends(require_access("daily_log:read")),
                  db: Session = Depends(get_db)):
    return _build_logs(db, [_visible_log_or_404(db, access, log_id)], with_deleted=access.is_org)[0]


@router.put("/daily-logs/{log_id}", response_model=DailyLogOut, responses={201: {"model": DailyLogOut}, **ERRORS})
def put_daily_log(log_id: uuid.UUID, body: DailyLogIn, response: Response,
                  access: Access = Depends(require_access("daily_log:write")), db: Session = Depends(get_db)):
    """Creates or updates the log of one child for one day. Stages that are sent are created or replaced,
    stages that are left out stay as they are, and stages sent as an explicit null are removed."""
    log = db.get(DailyLog, log_id)
    if log is None:
        if not access.can_see_child(body.child_id):
            raise HTTPException(404, "Child not found")
        child = lifecycle.get_or_404(db, Child, body.child_id, access)
        lifecycle.ensure_writable(child)
        log = DailyLog(id=log_id, child_id=child.id,
                       caregiver_id=access.user_id if access.role == "DOCENTE" else None)
        db.add(log)
        response.status_code = 201
    else:
        log = _visible_log_or_404(db, access, log_id)
        lifecycle.ensure_writable(log)
        if body.child_id != log.child_id:
            raise HTTPException(422, "A log can't move to another child")
        lifecycle.ensure_writable(db.get(Child, log.child_id))
    log.date, log.caregiver_name = body.date, body.caregiver_name
    db.flush()

    for name, model in PARTS:
        data = getattr(body, name)
        row = db.scalar(select(model).where(model.daily_log_id == log.id, lifecycle.live(model)))
        if data is None:
            if name in body.model_fields_set and row is not None:
                _remove_part(db, row)
            continue
        if model is CheckOut:
            _check_pickup(db, log, data, row)
        if row is None:
            db.add(model(id=uuid.uuid4(), daily_log_id=log.id, **data.model_dump()))
        else:
            for key, value in data.model_dump().items():
                setattr(row, key, value)
    db.commit()
    return _build_logs(db, [log])[0]


@router.delete("/daily-logs/{log_id}", status_code=204, responses=ERRORS)
def delete_daily_log(log_id: uuid.UUID, access: Access = Depends(require_access("daily_log:delete")),
                     db: Session = Depends(get_db)):
    """Soft delete of the log together with its stages, activities and photos (they are parts of it)."""
    log = _visible_log_or_404(db, access, log_id)
    if log.status != RecordStatus.DELETED:
        for act in db.scalars(select(ActivityRecord).where(
                ActivityRecord.daily_log_id == log.id, lifecycle.live(ActivityRecord))):
            _drop_images(db, act, keep=[])
        check_out = db.scalar(select(CheckOut).where(CheckOut.daily_log_id == log.id, lifecycle.live(CheckOut)))
        if check_out is not None and check_out.id_photo_file_id is not None:
            release(db.get(File, check_out.id_photo_file_id))
        lifecycle.soft_delete(db, log, parts=[(m, m.daily_log_id) for _, m in PARTS] +
                              [(ActivityRecord, ActivityRecord.daily_log_id)])
    db.commit()
    return Response(status_code=204)


# --- Activities (with photos) ------------------------------------------------------------------

def _drop_images(db: Session, act: ActivityRecord, keep: list[uuid.UUID]) -> None:
    for img in db.scalars(select(ActivityImage).where(
            ActivityImage.activity_record_id == act.id, lifecycle.live(ActivityImage))):
        if img.file_id not in keep:
            img.status = RecordStatus.DELETED
            release(db.get(File, img.file_id))


def _visible_activity_or_404(db: Session, access: Access, activity_id: uuid.UUID) -> tuple[ActivityRecord, DailyLog]:
    act = lifecycle.get_or_404(db, ActivityRecord, activity_id, access)
    return act, _visible_log_or_404(db, access, act.daily_log_id)


def _images_of(db: Session, act: ActivityRecord) -> list[ActivityImageOut]:
    return [ActivityImageOut.model_validate(i) for i in db.scalars(select(ActivityImage).where(
        ActivityImage.activity_record_id == act.id, lifecycle.live(ActivityImage)).order_by(ActivityImage.position))]


@router.get("/activities/{activity_id}", response_model=ActivityOut, responses=ERRORS)
def get_activity(activity_id: uuid.UUID, access: Access = Depends(require_access("daily_log:read")),
                 db: Session = Depends(get_db)):
    act, _ = _visible_activity_or_404(db, access, activity_id)
    return _activity_out(act, _images_of(db, act))


@router.put("/activities/{activity_id}", response_model=ActivityOut,
            responses={201: {"model": ActivityOut}, **ERRORS})
def put_activity(activity_id: uuid.UUID, body: ActivityIn, response: Response,
                 access: Access = Depends(require_access("daily_log:write")), db: Session = Depends(get_db)):
    """Creates or updates an activity. `image_file_ids` is the full ordered list of its photos (files uploaded with
    purpose ACTIVITY_IMAGE for this child); photos that are no longer listed are removed."""
    log = _visible_log_or_404(db, access, body.daily_log_id)
    lifecycle.ensure_writable(log)
    lifecycle.ensure_writable(db.get(Child, log.child_id))
    if len(set(body.image_file_ids)) != len(body.image_file_ids):
        raise HTTPException(422, "image_file_ids has duplicates")

    act = db.get(ActivityRecord, activity_id)
    if act is None:
        act = ActivityRecord(id=activity_id, daily_log_id=log.id)
        db.add(act)
        response.status_code = 201
    else:
        act, _ = _visible_activity_or_404(db, access, activity_id)
        lifecycle.ensure_writable(act)
        if act.daily_log_id != log.id:
            raise HTTPException(422, "An activity can't move to another log")
    act.name = body.name
    db.flush()

    current = {i.file_id: i for i in db.scalars(select(ActivityImage).where(
        ActivityImage.activity_record_id == act.id, lifecycle.live(ActivityImage)))}
    for position, file_id in enumerate(body.image_file_ids):
        if file_id in current:
            current[file_id].position = position
            continue
        load_attachable(db, file_id, FilePurpose.ACTIVITY_IMAGE, child_id=log.child_id, uploader=access.user_id)
        if db.scalar(select(ActivityImage.id).where(ActivityImage.file_id == file_id, lifecycle.live(ActivityImage))):
            raise HTTPException(409, f"File {file_id} is already used by another activity")
        db.add(ActivityImage(id=uuid.uuid4(), activity_record_id=act.id, file_id=file_id, position=position))
    _drop_images(db, act, keep=body.image_file_ids)
    db.commit()
    return _activity_out(act, _images_of(db, act))


@router.delete("/activities/{activity_id}", status_code=204, responses=ERRORS)
def delete_activity(activity_id: uuid.UUID, access: Access = Depends(require_access("daily_log:write")),
                    db: Session = Depends(get_db)):
    act, log = _visible_activity_or_404(db, access, activity_id)
    if act.status != RecordStatus.DELETED:
        lifecycle.ensure_writable(log)
        _drop_images(db, act, keep=[])
        act.status = RecordStatus.DELETED
    db.commit()
    return Response(status_code=204)
