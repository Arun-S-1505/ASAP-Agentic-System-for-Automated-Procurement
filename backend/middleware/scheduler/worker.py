"""
Background commit worker — periodically pushes approved decisions to the ERP.

Uses APScheduler to run inside the same FastAPI process
(no external task queue needed for the prototype).

Improvements over the original implementation:
    * **Double-commit guard** — re-reads the decision row inside its own
      session to confirm it is still ``pending_commit`` before submitting.
    * **ERP state verification** — after calling ``submit_approval`` the
      worker inspects the returned payload to confirm the transition.
    * **Detailed transition logging** — every state change is logged with
      before/after values so operators can reconstruct the timeline.
"""

import logging
from datetime import datetime

from apscheduler.schedulers.background import BackgroundScheduler
from sqlalchemy import select

from adapters.base import ERPAdapter
from adapters.mock_adapter import MockERPAdapter
from adapters.sap_adapter import SAPERPAdapter
from config import get_settings
from db.models import ApprovalDecision
from db.session import SessionLocal
from services.orchestrator import ApprovalOrchestrator
from services.notification_service import NotificationService

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# CommitWorker — Processes grace-period-expired decisions
# ---------------------------------------------------------------------------
class CommitWorker:
    """
    Background worker that commits staged approval decisions to the ERP.

    This worker runs periodically (via APScheduler), queries decisions
    whose grace period has expired, and submits them to the ERP system.

    Responsibilities:
        - Query pending_commit decisions (grace period expired)
        - *Re-verify* state before submission (double-commit guard)
        - Submit each decision to ERP via adapter
        - Verify ERP response confirms the transition
        - Update database state (committed / failed)
        - Handle errors gracefully (don't crash entire batch)
        - Log all state transitions with timestamps
    """

    # ERP response statuses that we accept as successful
    _ACCEPTED_ERP_STATUSES = frozenset({"ok", "simulated"})

    def __init__(self) -> None:
        self.orchestrator = ApprovalOrchestrator()
        self.notification_service = NotificationService()

    def run(self, adapter: ERPAdapter) -> dict[str, int]:
        """
        Execute the commit worker batch job.

        Pipeline:
            1. Open database session
            2. Query decisions ready for commit
            3. For each decision:
                a. Re-check state (double-commit prevention)
                b. Submit to ERP via adapter
                c. Verify ERP confirmation
                d. Update state to "committed"
                e. Set committed_at timestamp
                f. Handle failures gracefully
            4. Commit all database changes

        Args:
            adapter: ERP adapter instance (must be connected)

        Returns:
            Summary dict with commit statistics.
        """
        logger.info("CommitWorker: ── batch start ──")

        stats: dict[str, int] = {
            "total": 0,
            "committed": 0,
            "failed": 0,
            "skipped": 0,
        }

        with SessionLocal() as db:
            try:
                pending_decisions = self.orchestrator.list_pending_commits(db)
                stats["total"] = len(pending_decisions)

                if not pending_decisions:
                    logger.info("CommitWorker: no decisions ready for commit")
                    return stats

                logger.info(
                    "CommitWorker: found %d candidate(s)", len(pending_decisions),
                )

                for decision in pending_decisions:
                    try:
                        committed = self._process_single_decision(
                            decision=decision,
                            adapter=adapter,
                            db=db,
                        )
                        if committed:
                            stats["committed"] += 1
                        else:
                            stats["skipped"] += 1

                    except Exception as exc:
                        logger.error(
                            "CommitWorker: decision %s failed — %s",
                            decision.id,
                            exc,
                        )
                        self._mark_as_failed(decision, exc)
                        stats["failed"] += 1

                db.commit()

                logger.info(
                    "CommitWorker: ── batch end ── committed=%d  failed=%d  skipped=%d",
                    stats["committed"],
                    stats["failed"],
                    stats["skipped"],
                )

            except Exception as exc:
                logger.error("CommitWorker: batch-level failure — %s", exc)
                db.rollback()
                raise

        return stats

    # ------------------------------------------------------------------
    # Single-decision pipeline
    # ------------------------------------------------------------------

    def _process_single_decision(
        self,
        decision: ApprovalDecision,
        adapter: ERPAdapter,
        db,
    ) -> bool:
        """
        Process one decision.  Returns ``True`` if committed, ``False``
        if skipped (e.g. stale state).

        Raises:
            Exception: propagated when ERP submission hard-fails.
        """
        req_id = decision.erp_requisition_id

        # ── 1. Double-commit guard ──────────────────────────────────
        # Re-read from DB inside the same session to guard against race
        # conditions when two scheduler ticks overlap.
        fresh = db.execute(
            select(ApprovalDecision).where(ApprovalDecision.id == decision.id)
        ).scalar_one_or_none()

        if fresh is None or fresh.state != "pending_commit":
            logger.warning(
                "CommitWorker: decision %s no longer pending_commit (state=%s) — skipping",
                decision.id,
                fresh.state if fresh else "DELETED",
            )
            return False

        logger.info(
            "CommitWorker: ▸ processing  id=%s  req=%s  decision=%s",
            decision.id,
            req_id,
            decision.decision,
        )

        # ── 2. Submit to ERP ────────────────────────────────────────
        result = adapter.submit_approval(
            requisition_id=req_id,
            decision=decision.decision,
            comment=decision.comment or "",
        )
        logger.debug("CommitWorker: ERP response — %s", result)

        # ── 3. Validate ERP response ────────────────────────────────
        erp_status = result.get("status") if isinstance(result, dict) else None

        if erp_status == "already_processed":
            # The simulated ERP says this requisition was already
            # transitioned — treat as success to stay idempotent.
            logger.warning(
                "CommitWorker: ERP reports already_processed for %s — marking committed",
                req_id,
            )
        elif erp_status not in self._ACCEPTED_ERP_STATUSES:
            raise RuntimeError(
                f"ERP commit rejected: status={erp_status}, payload={result}"
            )

        # ── 4. Persist state transition ─────────────────────────────
        previous_state = decision.state
        decision.state = "committed"
        decision.committed_at = datetime.utcnow()

        logger.info(
            "CommitWorker: ✓ committed  id=%s  req=%s  [%s → %s]  erp_status=%s",
            decision.id,
            req_id,
            previous_state,
            decision.state,
            erp_status,
        )

        # ── 5. Dispatch post-commit notifications ────────────────────
        try:
            self.notification_service.send_post_commit_notifications(
                decision=decision,
                db=db,
            )
            logger.info(
                "CommitWorker: notifications dispatched for %s", req_id,
            )
        except Exception as notif_exc:
            # Notification failure must NOT block the commit
            logger.warning(
                "CommitWorker: notification dispatch failed for %s — %s",
                req_id,
                notif_exc,
            )

        return True

    # ------------------------------------------------------------------
    # Failure handling
    # ------------------------------------------------------------------

    @staticmethod
    def _mark_as_failed(decision: ApprovalDecision, exception: Exception) -> None:
        """Mark a decision as failed and record the error message."""
        previous_state = decision.state
        decision.state = "failed"
        decision.error_message = str(exception)[:500]

        logger.warning(
            "CommitWorker: ✗ failed  id=%s  req=%s  [%s → failed]  error=%s",
            decision.id,
            decision.erp_requisition_id,
            previous_state,
            decision.error_message,
        )


# ---------------------------------------------------------------------------
# Scheduler management
# ---------------------------------------------------------------------------
_scheduler: BackgroundScheduler | None = None
_commit_worker: CommitWorker | None = None
_adapter: ERPAdapter | None = None


def _commit_job() -> None:
    """
    Job callback: commit pending approval decisions to the ERP.

    This function will be called on a fixed interval by APScheduler.
    """
    global _commit_worker, _adapter

    if not _commit_worker or not _adapter:
        logger.warning("CommitWorker or adapter not initialized — skipping")
        return

    try:
        # Ensure adapter is connected (safe call, no private attribute access)
        try:
            logger.debug("Ensuring ERP adapter connection")
            _adapter.connect()
        except Exception as conn_exc:
            logger.warning("Adapter connect failed or already connected: %s", conn_exc)

        # Run commit worker
        stats = _commit_worker.run(_adapter)
        logger.info(
            "Commit job completed: total=%d, committed=%d, failed=%d, skipped=%d",
            stats["total"],
            stats["committed"],
            stats["failed"],
            stats.get("skipped", 0),
        )

    except Exception as exc:
        logger.error("Commit job failed with exception: %s", exc)


def start_scheduler() -> None:
    """
    Start the background scheduler.
    Called once during FastAPI application startup.
    """
    global _scheduler, _commit_worker, _adapter

    settings = get_settings()
    interval = settings.scheduler_commit_interval_seconds

    # Initialize commit worker
    _commit_worker = CommitWorker()

    # Initialize ERP adapter based on configuration
    if settings.erp_mode == "mock":
        logger.info("Using MockERPAdapter (erp_mode=mock, MySQL-backed)")
        _adapter = MockERPAdapter()
    elif settings.erp_mode == "sap":
        logger.info("Using SAPERPAdapter (erp_mode=sap)")
        _adapter = SAPERPAdapter()
    elif settings.erp_mode == "hybrid":
        from adapters.hybrid_adapter import HybridERPAdapter
        logger.info("Using HybridERPAdapter (erp_mode=hybrid, SAP reads + simulated writes)")
        _adapter = HybridERPAdapter()
    else:
        logger.warning(
            "Unknown erp_mode=%s, defaulting to MockERPAdapter",
            settings.erp_mode,
        )
        _adapter = MockERPAdapter()

    # Eagerly connect so that seed data (mock) or auth (SAP) is ready
    try:
        _adapter.connect()
        logger.info("ERP adapter connected on startup")
    except Exception as exc:
        logger.warning("ERP adapter initial connect failed: %s", exc)

    # Create and start scheduler
    _scheduler = BackgroundScheduler()
    _scheduler.add_job(
        _commit_job,
        trigger="interval",
        seconds=interval,
        id="erp_commit_worker",
        replace_existing=True,
    )
    _scheduler.start()

    logger.info(
        "Scheduler started — commit worker every %ds (adapter=%s)",
        interval,
        _adapter.__class__.__name__,
    )


def shutdown_scheduler() -> None:
    """
    Gracefully stop the scheduler.
    Called during FastAPI application shutdown.
    """
    global _scheduler, _adapter

    if _scheduler and _scheduler.running:
        _scheduler.shutdown(wait=False)
        logger.info("Scheduler shut down")

    if _adapter:
        try:
            _adapter.disconnect()
            logger.info("ERP adapter disconnected")
        except Exception as exc:
            logger.warning("Failed to disconnect adapter: %s", exc)

    _scheduler = None
    _commit_worker = None
    _adapter = None


# ---------------------------------------------------------------------------
# Helper functions for API routes
# ---------------------------------------------------------------------------

def is_scheduler_running() -> bool:
    """
    Check if the scheduler is currently running.
    
    Returns:
        True if scheduler is running, False otherwise.
    """
    global _scheduler
    return _scheduler is not None and _scheduler.running


def get_adapter() -> ERPAdapter | None:
    """
    Get the current ERP adapter instance.
    
    Returns:
        The ERP adapter instance, or None if not initialized.
    """
    global _adapter
    return _adapter


def set_adapter(mode: str) -> ERPAdapter:
    """
    Switch the global ERP adapter at runtime.

    Disconnects the current adapter, creates a new one based on `mode`,
    connects it, and replaces the global reference.

    Args:
        mode: "mock" or "sap"

    Returns:
        The newly created and connected adapter.

    Raises:
        ValueError: If mode is not recognised.
    """
    global _adapter

    # Tear down current adapter
    if _adapter:
        try:
            _adapter.disconnect()
        except Exception as exc:
            logger.warning("Error disconnecting old adapter: %s", exc)

    # Create new adapter
    if mode == "mock":
        logger.info("Switching to MockERPAdapter")
        _adapter = MockERPAdapter()
    elif mode == "sap":
        from adapters.sap_adapter import SAPERPAdapter
        logger.info("Switching to SAPERPAdapter")
        _adapter = SAPERPAdapter()
    elif mode == "hybrid":
        from adapters.hybrid_adapter import HybridERPAdapter
        logger.info("Switching to HybridERPAdapter")
        _adapter = HybridERPAdapter()
    else:
        raise ValueError(f"Unknown erp_mode: {mode!r}. Use 'mock', 'sap', or 'hybrid'.")

    _adapter.connect()
    logger.info("New %s adapter connected", mode)
    return _adapter


