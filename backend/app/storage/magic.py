"""Check that an object's first bytes match its declared content type (a "jpeg" must start like a JPEG)."""

DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
HEAD_BYTES = 16


def matches(content_type: str, head: bytes) -> bool:
    if content_type == "image/jpeg":
        return head.startswith(b"\xff\xd8\xff")
    if content_type == "image/png":
        return head.startswith(b"\x89PNG\r\n\x1a\n")
    if content_type == "image/webp":
        return head[:4] == b"RIFF" and head[8:12] == b"WEBP"
    if content_type == "application/pdf":
        return head.startswith(b"%PDF-")
    if content_type == DOCX:  # a docx is a zip; this can't tell it from other zips, which is fine
        return head.startswith(b"PK\x03\x04")
    return False


EXTENSIONS = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
    "application/pdf": "pdf",
    DOCX: "docx",
}
