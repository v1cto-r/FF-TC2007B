class StorageError(Exception):
    pass


class StorageForbidden(StorageError):
    pass


class InvalidUpload(StorageError):
    pass


class RateLimitExceeded(StorageError):
    def __init__(self, retry_after: int):
        super().__init__("Rate limit exceeded")
        self.retry_after = retry_after
