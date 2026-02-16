"""
SQLAlchemy session factory and engine configuration.

Provides:
    - engine           — global SQLAlchemy engine (connection pool)
    - SessionLocal     — scoped session factory
    - get_db()         — FastAPI dependency that yields a DB session
"""

from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session

from config import get_settings

settings = get_settings()

# ---------------------------------------------------------------------------
# Engine — single connection pool shared across the application
# ---------------------------------------------------------------------------
engine = create_engine(
    settings.database_url,
    echo=settings.debug,        # SQL logging in dev mode
    pool_pre_ping=True,         # verify connections before use
    pool_size=10,
    max_overflow=20,
)

# ---------------------------------------------------------------------------
# Session factory
# ---------------------------------------------------------------------------
SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine,
)


# ---------------------------------------------------------------------------
# FastAPI dependency
# ---------------------------------------------------------------------------
def get_db() -> Generator[Session, None, None]:
    """
    Yield a database session for a single request.
    Automatically closes the session when the request ends.

    Usage in FastAPI:
        @router.get("/items")
        def list_items(db: Session = Depends(get_db)):
            ...
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
