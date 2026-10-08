from functools import lru_cache

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

    secret_key: str
    jwt_algorithm: str = "HS256"

    # Either one makes the starter load sample data into an empty database
    debug: bool = False
    test: bool = False

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
