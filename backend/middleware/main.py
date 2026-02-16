"""
Intelligent ERP Approval Middleware
====================================
Single-service FastAPI application.

Startup sequence:
    1. Load configuration from .env
    2. Create database tables (dev only — use Alembic in prod)
    3. Start background scheduler
    4. Mount API routes
    5. Serve with Uvicorn

Run:
    uvicorn main:app --reload          # development
    uvicorn main:app --host 0.0.0.0    # production-like
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import get_settings
from db import Base, engine
from scheduler import start_scheduler, shutdown_scheduler, init_adapter
from api import router as api_router
from api.auth_routes import router as auth_router
from api.analytics_routes import router as analytics_router
from api.batch_routes import router as batch_router
import db.user_model  # noqa: F401 — ensure User table is created

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
settings = get_settings()

logging.basicConfig(
    level=logging.DEBUG if settings.debug else logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
)
logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Application lifespan (startup / shutdown hooks)
# ---------------------------------------------------------------------------
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Runs once on startup — and the code after `yield` runs on shutdown.
    Replaces the deprecated @app.on_event("startup") / ("shutdown") pattern.
    """
    # ---- STARTUP ----
    logger.info("Starting %s (env=%s)", settings.app_name, settings.app_env)

    # Create tables if they don't exist (idempotent operation)
    try:
        logger.info("Ensuring database tables exist (creating if needed)")
        Base.metadata.create_all(bind=engine)
        logger.info("Database tables ready")
    except Exception as exc:
        logger.warning("Database table creation failed: %s", exc)

    # Demo mode: Clear all POs for fresh demo
    if settings.demo_mode:
        logger.warning("DEMO_MODE enabled — resetting all requisitions and decisions")
        from db.session import SessionLocal
        from db.models import Requisition, ApprovalDecision, ERPSimulatedRequisition
        db = SessionLocal()
        try:
            db.query(ApprovalDecision).delete()
            db.query(Requisition).delete()
            db.query(ERPSimulatedRequisition).delete()
            db.commit()
            logger.info("Demo reset complete — all POs cleared")
        except Exception as exc:
            logger.error("Demo reset failed: %s", exc)
            db.rollback()
        finally:
            db.close()

    # Always initialize ERP adapter (needed for /detect endpoint)
    init_adapter()

    # Start background commit worker (only if auto-commit enabled)
    if settings.auto_commit_enabled:
        start_scheduler()
        logger.info("Auto-commit scheduler started")
    else:
        logger.warning("AUTO_COMMIT_ENABLED=false — scheduler NOT started, POs will stay pending")
    logger.info("Application startup complete")

    yield  # ---- APPLICATION RUNNING ----

    # ---- SHUTDOWN ----
    shutdown_scheduler()
    logger.info("Application shutdown complete")


# ---------------------------------------------------------------------------
# FastAPI application instance
# ---------------------------------------------------------------------------
app = FastAPI(
    title="Intelligent ERP Approval Middleware",
    description=(
        "Research prototype: risk-scoring and automated approval pipeline "
        "for SAP purchase requisitions."
    ),
    version="0.1.0",
    lifespan=lifespan,
)

# Mount API routes under /api/v1
app.include_router(api_router, prefix="/api/v1")
app.include_router(auth_router, prefix="/api/v1")
app.include_router(analytics_router, prefix="/api/v1")
app.include_router(batch_router, prefix="/api/v1")

# CORS — allow mobile app and web clients to connect
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------------------------
# Root endpoint (convenience)
# ---------------------------------------------------------------------------
@app.get("/", tags=["root"])
def root():
    return {
        "service": settings.app_name,
        "version": "0.1.0",
        "docs": "/docs",
    }
