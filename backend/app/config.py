from functools import lru_cache

from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    postgres_user: str
    postgres_password: str
    postgres_db: str
    db_host: str = "localhost"
    db_port: int = 5432

    s3_access_key: str
    s3_secret_key: str
    s3_host: str = "localhost"
    s3_port: int = 9000
    s3_region: str = "us-east-1"
    # Host the *client* reaches; presigned URLs are signed against it
    s3_public_endpoint: str = "http://localhost:5003"

    storage_policy_path: str = "app/storage/policy.yml"
    storage_max_image_bytes: int = 5 * 1024 * 1024
    storage_max_file_bytes: int = 10 * 1024 * 1024
    storage_upload_per_minute: int = 10
    storage_upload_per_day: int = 200
    storage_download_per_minute: int = 300
    storage_upload_expires: int = 300
    storage_download_expires: int = 900

    # Two different symmetric keys: a leaked session key can't forge refresh tokens
    session_secret_key: str
    refresh_secret_key: str
    jwt_algorithm: str = "HS256"
    session_ttl_minutes: int = 5
    refresh_ttl_days: int = 7

    # The chat socket is closed after this many seconds without a frame (clients send a ping every ~30 s)
    chat_idle_seconds: float = 90

    # Firebase service-account key (JSON file) for push notifications. Unset = pushes are off.
    firebase_credentials_path: str | None = None

    # Port of the Prometheus /metrics server, separate from the API port so it is never public. Unset = not served
    metrics_port: int | None = None

    # Created on start if there is no ORG user yet. Must be changed on first login.
    default_org_email: str = "admin"
    default_org_password: str = "admin"

    # Either one makes the starter load sample data into an empty database
    debug: bool = False
    test: bool = False

    @model_validator(mode="after")
    def _keys_differ(self):
        if self.session_secret_key == self.refresh_secret_key:
            raise ValueError("SESSION_SECRET_KEY and REFRESH_SECRET_KEY must be different")
        return self

    @property
    def s3_endpoint(self) -> str:
        return f"http://{self.s3_host}:{self.s3_port}"

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg2://{self.postgres_user}:{self.postgres_password}"
            f"@{self.db_host}:{self.db_port}/{self.postgres_db}"
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
