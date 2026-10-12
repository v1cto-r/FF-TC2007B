class StorageError(Exception):
    pass


class InvalidUpload(StorageError):
    """The requested type / size isn't acceptable, or the uploaded bytes don't match what was declared."""


class ObjectMissing(StorageError):
    """The client never uploaded the object."""
