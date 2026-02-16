"""
ApprovalOrchestrator — Top-level service that wires adapters → core → persistence.

This is the main entry point the API layer and scheduler call.
It owns the workflow:
    1. Fetch requisitions from ERP (via adapter)
    2. Score risk (via core engine)
    3. Decide approval (via core engine)
    4. Persist decision (via db layer)
    5. Optionally commit decision back to ERP (via adapter)
"""

import logging
import uuid
from datetime import datetime, timedelta
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from adapters.base import ERPAdapter
from core.risk_scoring import RiskScoringEngine
from core.risk_explanation import RiskExplanationEngine
from db.models import ApprovalDecision
from models.domain import RequisitionDTO
from config import get_settings

logger = logging.getLogger(__name__)


class ApprovalOrchestrator:
    """
    Stateless orchestrator — handles detection, staging, and lifecycle management
    of approval decisions.

    This orchestrator does NOT commit to ERP — it only stages decisions.
    The scheduler is responsible for committing staged decisions after grace period.

    Usage:
        from db.session import SessionLocal
        from adapters import MockERPAdapter
        
        adapter = MockERPAdapter()
        adapter.connect()
        orchestrator = ApprovalOrchestrator()
        
        with SessionLocal() as db:
            staged = orchestrator.detect_and_stage_requisitions(adapter, db)
            print(f"Staged {len(staged)} decisions")
    """

    # Risk score thresholds for decision logic
    LOW_RISK_THRESHOLD: float = 30.0
    MEDIUM_RISK_THRESHOLD: float = 70.0

    def __init__(
        self,
        risk_engine: RiskScoringEngine | None = None,
        explanation_engine: RiskExplanationEngine | None = None,
    ) -> None:
        self.risk_engine = risk_engine or RiskScoringEngine()
        self.explanation_engine = explanation_engine or RiskExplanationEngine()
        self.settings = get_settings()

    # ------------------------------------------------------------------
    # 1. Detection & Staging
    # ------------------------------------------------------------------

    def detect_and_stage_requisitions(
        self,
        adapter: ERPAdapter,
        db: Session,
    ) -> list[ApprovalDecision]:
        """
        Fetch pending requisitions from ERP, score them, make decisions,
        and stage for commit (with grace period).

        Pipeline:
            1. Fetch pending requisitions from ERP via adapter
            2. For each requisition:
                a. Check if already has active pending decision (avoid duplicates)
                b. Compute risk score
                c. Determine decision based on risk
                d. Create ApprovalDecision record in DB
                e. Set state = "pending_commit"
                f. Set commit_at = now + grace_period
            3. Return list of staged decisions

        Args:
            adapter: ERP adapter instance (must be connected)
            db: SQLAlchemy session

        Returns:
            List of newly created ApprovalDecision instances.
        """
        logger.info("Starting detect_and_stage_requisitions workflow")

        # Fetch pending requisitions from ERP
        try:
            requisitions = adapter.fetch_pending_requisitions()
            logger.info("Fetched %d requisitions from ERP", len(requisitions))
        except Exception as exc:
            logger.error("Failed to fetch requisitions from ERP: %s", exc)
            return []

        if not requisitions:
            logger.info("No pending requisitions found")
            return []

        staged_decisions: list[ApprovalDecision] = []

        for requisition in requisitions:
            try:
                # Check for existing active decisions (avoid duplicates)
                if self._has_active_decision(requisition.erp_requisition_id, db):
                    logger.debug(
                        "Skipping %s — already has active decision",
                        requisition.erp_requisition_id,
                    )
                    continue

                # Compute risk score
                risk_score = self.risk_engine.score(requisition)
                logger.debug(
                    "Risk score for %s: %.2f",
                    requisition.erp_requisition_id,
                    risk_score,
                )

                # Generate human-readable risk explanation
                risk_explanation = self.explanation_engine.explain(
                    requisition, risk_score,
                )
                logger.debug(
                    "Risk explanation for %s: %s",
                    requisition.erp_requisition_id,
                    risk_explanation,
                )

                # Determine decision based on risk thresholds
                decision = self._determine_decision(risk_score)

                # Set state based on auto-commit configuration
                # Always compute commit_at (required field), but only use pending_commit state when auto-commit is enabled
                commit_at = datetime.utcnow() + timedelta(
                    minutes=self.settings.grace_period_minutes
                )
                if self.settings.auto_commit_enabled:
                    # Auto-commit: schedule for ERP commit after grace period
                    state = "pending_commit"
                else:
                    # Manual mode: leave as detected for user action
                    state = "detected"

                # Create approval decision record
                approval_decision = ApprovalDecision(
                    id=str(uuid.uuid4()),
                    erp_requisition_id=requisition.erp_requisition_id,
                    risk_score=risk_score,
                    risk_explanation=risk_explanation,
                    decision=decision,
                    state=state,
                    commit_at=commit_at,
                    comment=self._generate_comment(risk_score, decision),
                )

                db.add(approval_decision)
                staged_decisions.append(approval_decision)

                logger.info(
                    "Staged decision for %s: %s (risk=%.2f, commit_at=%s)",
                    requisition.erp_requisition_id,
                    decision,
                    risk_score,
                    commit_at.isoformat(),
                )

            except Exception as exc:
                logger.error(
                    "Failed to process requisition %s: %s",
                    requisition.erp_requisition_id,
                    exc,
                )
                continue

        # Commit all staged decisions to DB
        try:
            db.commit()
            logger.info("Successfully staged %d decisions", len(staged_decisions))
        except Exception as exc:
            logger.error("Failed to commit staged decisions: %s", exc)
            db.rollback()
            return []

        return staged_decisions

    # ------------------------------------------------------------------
    # 2. Cancellation / Undo
    # ------------------------------------------------------------------

    def undo_decision(
        self,
        erp_requisition_id: str,
        db: Session,
    ) -> ApprovalDecision | None:
        """
        Cancel a pending commit decision (grace period undo).

        This allows users to cancel an auto-approval before it's
        committed to the ERP system.

        Args:
            erp_requisition_id: ERP requisition identifier
            db: SQLAlchemy session

        Returns:
            The cancelled ApprovalDecision, or None if not found.
        """
        logger.info("Attempting to undo decision for %s", erp_requisition_id)

        # Find pending_commit or detected decision
        stmt = (
            select(ApprovalDecision)
            .where(ApprovalDecision.erp_requisition_id == erp_requisition_id)
            .where(ApprovalDecision.state.in_(["pending_commit", "detected"]))
            .order_by(ApprovalDecision.created_at.desc())
            .limit(1)
        )

        decision = db.execute(stmt).scalar_one_or_none()

        if not decision:
            logger.warning(
                "No pending_commit decision found for %s",
                erp_requisition_id,
            )
            return None

        # Cancel the decision
        decision.state = "cancelled"
        decision.comment = (
            f"{decision.comment or ''} [Cancelled by user]".strip()
        )

        try:
            db.commit()
            logger.info(
                "Successfully cancelled decision for %s (id=%s)",
                erp_requisition_id,
                decision.id,
            )
            return decision
        except Exception as exc:
            logger.error("Failed to cancel decision: %s", exc)
            db.rollback()
            return None

    # ------------------------------------------------------------------
    # 3. Query for Commit-Ready Decisions
    # ------------------------------------------------------------------

    def list_pending_commits(
        self,
        db: Session,
    ) -> list[ApprovalDecision]:
        """
        Query decisions that are ready to be committed to ERP.

        Criteria:
            - state = "pending_commit"
            - commit_at <= now (grace period expired)

        Args:
            db: SQLAlchemy session

        Returns:
            List of ApprovalDecision instances ready for commit.
        """
        now = datetime.utcnow()

        stmt = (
            select(ApprovalDecision)
            .where(ApprovalDecision.state == "pending_commit")
            .where(ApprovalDecision.commit_at <= now)
            .order_by(ApprovalDecision.commit_at)
        )

        decisions = db.execute(stmt).scalars().all()

        logger.info(
            "Found %d decisions ready for commit (grace period expired)",
            len(decisions),
        )

        return list(decisions)

    # ------------------------------------------------------------------
    # 4. Manual Approve / Reject
    # ------------------------------------------------------------------

    def approve_decision(
        self,
        erp_requisition_id: str,
        db: Session,
        comment: str | None = None,
    ) -> ApprovalDecision | None:
        """
        Manually approve a pending requisition.

        Transitions the decision from ``pending_commit`` → ``committed``
        and sets decision to ``manual_approve``.  If the decision was
        already ``auto_approve`` it stays that way — only ``hold`` /
        ``manual_approve`` decisions need this path.

        Args:
            erp_requisition_id: ERP requisition identifier
            db: SQLAlchemy session
            comment: Optional manager comment

        Returns:
            The updated ApprovalDecision, or None if not found.
        """
        logger.info("Approving decision for %s", erp_requisition_id)

        decision = self._find_pending_decision(erp_requisition_id, db)
        if not decision:
            return None

        # Update decision fields
        decision.decision = "manual_approve"
        decision.state = "committed"
        decision.committed_at = datetime.utcnow()
        if comment:
            decision.comment = (
                f"{decision.comment or ''} | Approved: {comment}".strip(" |")
            )
        else:
            decision.comment = (
                f"{decision.comment or ''} | Manually approved by manager".strip(" |")
            )

        try:
            db.commit()
            logger.info(
                "Decision approved for %s (id=%s)",
                erp_requisition_id,
                decision.id,
            )
            return decision
        except Exception as exc:
            logger.error("Failed to approve decision: %s", exc)
            db.rollback()
            return None

    def reject_decision(
        self,
        erp_requisition_id: str,
        db: Session,
        comment: str | None = None,
    ) -> ApprovalDecision | None:
        """
        Manually reject a pending requisition.

        Transitions the decision from ``pending_commit`` → ``committed``
        and sets decision to ``reject``.

        Args:
            erp_requisition_id: ERP requisition identifier
            db: SQLAlchemy session
            comment: Optional rejection reason

        Returns:
            The updated ApprovalDecision, or None if not found.
        """
        logger.info("Rejecting decision for %s", erp_requisition_id)

        decision = self._find_pending_decision(erp_requisition_id, db)
        if not decision:
            return None

        # Update decision fields
        decision.decision = "reject"
        decision.state = "committed"
        decision.committed_at = datetime.utcnow()
        if comment:
            decision.comment = (
                f"{decision.comment or ''} | Rejected: {comment}".strip(" |")
            )
        else:
            decision.comment = (
                f"{decision.comment or ''} | Manually rejected by manager".strip(" |")
            )

        try:
            db.commit()
            logger.info(
                "Decision rejected for %s (id=%s)",
                erp_requisition_id,
                decision.id,
            )
            return decision
        except Exception as exc:
            logger.error("Failed to reject decision: %s", exc)
            db.rollback()
            return None

    # ------------------------------------------------------------------
    # Private Helper Methods
    # ------------------------------------------------------------------

    def _find_pending_decision(
        self,
        erp_requisition_id: str,
        db: Session,
    ) -> ApprovalDecision | None:
        """Find the latest pending decision (pending_commit or detected) for a requisition."""
        stmt = (
            select(ApprovalDecision)
            .where(ApprovalDecision.erp_requisition_id == erp_requisition_id)
            .where(ApprovalDecision.state.in_(["pending_commit", "detected"]))
            .order_by(ApprovalDecision.created_at.desc())
            .limit(1)
        )
        decision = db.execute(stmt).scalar_one_or_none()
        if not decision:
            logger.warning(
                "No pending decision found for %s",
                erp_requisition_id,
            )
        return decision

    def _has_active_decision(
        self,
        erp_requisition_id: str,
        db: Session,
    ) -> bool:
        """
        Check if requisition already has an active (non-terminal) decision.

        Active states: pending_commit, detected
        Terminal states: committed, cancelled, failed

        Args:
            erp_requisition_id: ERP requisition identifier
            db: SQLAlchemy session

        Returns:
            True if active decision exists, False otherwise.
        """
        stmt = (
            select(ApprovalDecision)
            .where(ApprovalDecision.erp_requisition_id == erp_requisition_id)
            .where(
                ApprovalDecision.state.in_(["pending_commit", "detected"])
            )
            .limit(1)
        )

        result = db.execute(stmt).scalar_one_or_none()
        return result is not None

    def _determine_decision(self, risk_score: float) -> str:
        """
        Map risk score to approval decision.

        Decision logic:
            risk_score < 30  → auto_approve (low risk)
            risk_score < 70  → manual_approve (medium risk, needs review)
            risk_score >= 70 → hold (high risk, needs investigation)

        Args:
            risk_score: Computed risk value (0-100)

        Returns:
            Decision string: "auto_approve" | "manual_approve" | "hold"
        """
        if risk_score < self.LOW_RISK_THRESHOLD:
            return "auto_approve"
        elif risk_score < self.MEDIUM_RISK_THRESHOLD:
            return "manual_approve"
        else:
            return "hold"

    def _generate_comment(self, risk_score: float, decision: str) -> str:
        """
        Generate human-readable comment explaining the decision.

        Args:
            risk_score: Computed risk value
            decision: Approval decision

        Returns:
            Explanation string.
        """
        if decision == "auto_approve":
            return f"Low risk (score: {risk_score:.2f}) — auto-approved"
        elif decision == "manual_approve":
            return f"Medium risk (score: {risk_score:.2f}) — requires approval"
        else:
            return f"High risk (score: {risk_score:.2f}) — flagged for review"
