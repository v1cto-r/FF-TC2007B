import uuid

from botocore.exceptions import BotoCoreError, ClientError
from fastapi import APIRouter, Depends, HTTPException, Request, Response
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.access import Access
from app.auth.dependency import require_access
from app.database.connection import get_db
from app.database.lifecycle import ensure_writable, get_or_404
from app.database.models import Child, ChildDocument, File, FilePurpose, RecordStatus, UploadState
from app.rate_limit import hit
from app.schemas.common import Detail
from app.schemas.files import (
    DocumentIn, DocumentOut, FileCreate, FileDownload, FileOut, SignedUpload, UploadTicket,
)
from app.storage import InvalidUpload, StorageService, magic
from app.storage.magic import EXTENSIONS
from app.storage.rules import (
    authorize_upload, can_read_file, expire_pending, file_in_use, load_attachable, release,
)

router = APIRouter(tags=["files"])


def get_storage(request: Request) -> StorageService:
    return request.app.state.storage


def _s3_unavailable(e: Exception) -> HTTPException:
    return HTTPException(503, "File storage is unavailable, try again later")


def _signed(storage: StorageService, f: File) -> SignedUpload:
    s = storage.presign_put(f.bucket, f.key, f.content_type, f.size)
    return SignedUpload(url=s.url, headers=s.headers, expires_at=s.expires_at)


def _charge_upload(db: Session, storage: StorageService, user_id: uuid.UUID) -> None:
    limits = storage.policy.rate_limits.upload
    if limits.per_minute:
        hit(db, f"file-upload-min:user:{user_id}", limits.per_minute, 60)
    if limits.per_day:
        hit(db, f"file-upload-day:user:{user_id}", limits.per_day, 86400)


@router.post("/files", response_model=UploadTicket, status_code=201,
             responses={200: {"model": UploadTicket, "description": "Retry of a pending upload: a fresh URL"},
                        403: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail},
                        429: {"model": Detail}})
def create_upload(body: FileCreate, response: Response, access: Access = Depends(require_access("file:write")),
                  storage: StorageService = Depends(get_storage), db: Session = Depends(get_db)):
    """Step 1 of an upload: registers the file (PENDING) and returns a signed URL to PUT the bytes to."""
    try:
        bucket, kind = storage.policy.resolve(body.purpose, body.content_type, body.size)
    except InvalidUpload as e:
        raise HTTPException(422, str(e))

    existing = db.get(File, body.id)
    if existing is not None:  # the app retrying after a lost response
        same = (existing.uploaded_by == access.user_id and existing.purpose == body.purpose
                and existing.content_type == body.content_type and existing.size == body.size)
        if not same or existing.upload_state != UploadState.PENDING or existing.status is not None:
            raise HTTPException(409, "This file id is already used")
        _charge_upload(db, storage, access.user_id)
        response.status_code = 200
        return UploadTicket(file=existing, upload=_signed(storage, existing))

    target = authorize_upload(db, access, body.purpose, body.target_id)
    _charge_upload(db, storage, access.user_id)  # after authorization, so denied requests don't use quota
    expire_pending(db, storage.policy.upload_expires)

    ext = EXTENSIONS.get(body.content_type, "bin")
    f = File(id=body.id, bucket=bucket, kind=kind, purpose=body.purpose, content_type=body.content_type,
             size=body.size, original_name=body.original_name, upload_state=UploadState.PENDING,
             uploaded_by=access.user_id, estancia_id=target.estancia_id, child_id=target.child_id,
             peer_id=target.peer_id,
             key=f"{body.purpose.value.lower()}/{target.owner_id}/{body.id}.{ext}")
    db.add(f)
    db.commit()
    return UploadTicket(file=f, upload=_signed(storage, f))


@router.post("/files/{file_id}/complete", response_model=FileOut,
             responses={404: {"model": Detail}, 409: {"model": Detail}, 422: {"model": Detail}})
def complete_upload(file_id: uuid.UUID, access: Access = Depends(require_access("file:write")),
                    storage: StorageService = Depends(get_storage), db: Session = Depends(get_db)):
    """Step 3: after the PUT, the server checks the object (size, type, first bytes) and marks it READY."""
    f = db.get(File, file_id)
    if f is None or f.uploaded_by != access.user_id or f.status is not None:
        raise HTTPException(404, "File not found")
    if f.upload_state == UploadState.READY:
        return f
    try:
        info = storage.head(f.bucket, f.key)
        if info is None:
            raise HTTPException(409, "The upload hasn't arrived yet")
        stored_type = info.content_type.split(";")[0].strip().lower()
        valid = info.size == f.size and stored_type == f.content_type.lower()
        if valid:
            valid = magic.matches(f.content_type, storage.read_head(f.bucket, f.key))
    except (BotoCoreError, ClientError) as e:
        raise _s3_unavailable(e)
    if not valid:
        f.status = RecordStatus.DELETED  # kept for audit, never served
        db.commit()
        raise HTTPException(422, "The uploaded file doesn't match what was declared")
    f.upload_state = UploadState.READY
    db.commit()
    return f


@router.get("/files/{file_id}", response_model=FileDownload,
            responses={404: {"model": Detail}, 429: {"model": Detail}})
def get_file(file_id: uuid.UUID, access: Access = Depends(require_access("file:read")),
             storage: StorageService = Depends(get_storage), db: Session = Depends(get_db)):
    """Metadata plus a signed GET URL. DELETED files are only served to ORG."""
    f = db.get(File, file_id)
    if f is None or f.upload_state != UploadState.READY or not can_read_file(db, access, f):
        raise HTTPException(404, "File not found")
    limit = storage.policy.rate_limits.download.per_minute
    if limit:
        hit(db, f"file-download:user:{access.user_id}", limit, 60)
    s = storage.presign_get(f.bucket, f.key)
    return FileDownload(file=f, url=s.url, expires_at=s.expires_at)


@router.delete("/files/{file_id}", status_code=204,
               responses={404: {"model": Detail}, 409: {"model": Detail}})
def delete_file(file_id: uuid.UUID, access: Access = Depends(require_access("file:delete")),
                db: Session = Depends(get_db)):
    """Removes a file you uploaded that isn't attached to anything. Attached files go with their record."""
    f = db.get(File, file_id)
    if f is None or f.uploaded_by != access.user_id or f.status is not None:
        raise HTTPException(404, "File not found")
    if file_in_use(db, f.id):
        raise HTTPException(409, "The file is attached; remove it from its record instead")
    release(f)
    db.commit()
    return Response(status_code=204)


# --- Child documents (PDF / DOCX / scans) -----------------------------------------------------

@router.get("/children/{child_id}/documents", response_model=list[DocumentOut], tags=["documents"])
def list_documents(child_id: uuid.UUID, include_deleted: bool = False,
                   access: Access = Depends(require_access("document:read")), db: Session = Depends(get_db)):
    if not access.can_see_child(child_id):
        raise HTTPException(404, "Child not found")
    stmt = select(ChildDocument).where(ChildDocument.child_id == child_id).order_by(ChildDocument.created_at)
    if include_deleted:
        if not access.is_org:
            raise HTTPException(403, "Only ORG can list deleted records")
    else:
        stmt = stmt.where(ChildDocument.status.is_distinct_from(RecordStatus.DELETED))
    return db.scalars(stmt).all()


def _parent_of(access: Access, child_id: uuid.UUID) -> None:
    if access.role != "PADRE":
        raise HTTPException(403, "Only the child's parent manages their documents")
    if child_id not in access.children:
        raise HTTPException(404, "Child not found")


@router.put("/documents/{document_id}", response_model=DocumentOut, tags=["documents"],
            responses={201: {"model": DocumentOut}, 403: {"model": Detail}, 404: {"model": Detail},
                       409: {"model": Detail}, 422: {"model": Detail}})
def put_document(document_id: uuid.UUID, body: DocumentIn, response: Response,
                 access: Access = Depends(require_access("document:write")), db: Session = Depends(get_db)):
    """Attaches an uploaded file (purpose CHILD_DOCUMENT) to the child. Only the title can change afterwards."""
    _parent_of(access, body.child_id)
    child = get_or_404(db, Child, body.child_id, access)
    ensure_writable(child)
    doc = db.get(ChildDocument, document_id)
    if doc is None:
        f = load_attachable(db, body.file_id, FilePurpose.CHILD_DOCUMENT, child_id=child.id, uploader=access.user_id)
        doc = ChildDocument(id=document_id, child_id=child.id, file_id=f.id, title=body.title)
        db.add(doc)
        response.status_code = 201
    else:
        if doc.child_id != body.child_id or not access.can_see_child(doc.child_id):
            raise HTTPException(404, "Document not found")
        ensure_writable(doc)
        if doc.file_id != body.file_id:
            raise HTTPException(422, "A document's file can't be replaced; delete it and add a new one")
        doc.title = body.title
    db.commit()
    return doc


@router.delete("/documents/{document_id}", status_code=204, tags=["documents"],
               responses={403: {"model": Detail}, 404: {"model": Detail}})
def delete_document(document_id: uuid.UUID, access: Access = Depends(require_access("document:delete")),
                    db: Session = Depends(get_db)):
    doc = db.get(ChildDocument, document_id)
    if doc is None or doc.status == RecordStatus.DELETED:
        raise HTTPException(404, "Document not found")
    _parent_of(access, doc.child_id)
    doc.status = RecordStatus.DELETED
    release(db.get(File, doc.file_id))
    db.commit()
    return Response(status_code=204)
