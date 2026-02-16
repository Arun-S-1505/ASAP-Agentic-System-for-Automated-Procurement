"""
Configuration module for ERP Approval Middleware.
Loads settings from environment variables / .env file using pydantic-settings.
"""

from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    Application settings.
    Values are read from environment variables first, then from .env file.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    # --- Application ---
    app_name: str = "erp-approval-middleware"
    app_env: str = "development"  # development | staging | production
    debug: bool = True

    # --- Database (PostgreSQL) ---
    db_host: str = "localhost"
    db_port: int = 5432
    db_user: str = "postgres"
    db_password: str = "changeme"
    db_name: str = "erp_middleware"

    # --- SAP Connection ---
    sap_base_url: str = "https://sandbox.api.sap.com"
    sap_username: str = ""
    sap_password: str = ""
    sap_api_key: str = ""  # SAP API Business Hub sandbox key
    sap_timeout: int = 30
    sap_service_prefix: str = ""  # "/s4hanacloud" for API Hub Sandbox, "" for on-premise/BTP

    # --- ERP Adapter Mode ---
    erp_mode: str = "mock"  # mock | sap | hybrid

    # --- Scheduler ---
    scheduler_commit_interval_seconds: int = 60

    # --- Approval Grace Period ---
    grace_period_minutes: int = 5  # Time window before auto-committing to ERP

    # --- Demo Mode ---
    demo_mode: bool = False  # Reset POs on startup for demos

    # --- Auto-Commit ---
    auto_commit_enabled: bool = True  # Set False to disable automatic ERP commit

    # --- API ---
    api_host: str = "0.0.0.0"
    api_port: int = 8000

    # --- JWT Authentication ---
    jwt_secret_key: str = "asap-super-secret-key-change-in-production"
    jwt_algorithm: str = "HS256"
    jwt_expiry_minutes: int = 480  # 8 hours

    @property
    def database_url(self) -> str:
        """Build SQLAlchemy-compatible PostgreSQL connection string."""
        return (
            f"postgresql+psycopg2://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )

    @property
    def is_production(self) -> bool:
        return self.app_env == "production"


@lru_cache
def get_settings() -> Settings:
    """
    Cached settings singleton.
    Call this instead of instantiating Settings() directly.
    """
    return Settings()
