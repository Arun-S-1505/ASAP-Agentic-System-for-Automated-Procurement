"""
FastAPI route definitions.

All endpoints are mounted under the /api/v1 prefix (see main.py).
Routes are thin — they delegate to the services layer immediately.
"""

import logging
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from config import get_settings
from core.auth import get_current_user
from db.models import ApprovalDecision, NotificationLog
from db.session import get_db
from models.schemas import (
    ApprovalDecisionOut,
    ApproveRejectRequest,
    ApproveRejectResponse,
    DecisionListResponse,
    DetectResponse,
    HealthResponse,
    NotificationListResponse,
    NotificationLogOut,
    UndoResponse,
)
from scheduler.worker import get_adapter, is_scheduler_running, set_adapter
from services.orchestrator import ApprovalOrchestrator

logger = logging.getLogger(__name__)

router = APIRouter()
settings = get_settings()


# ---------------------------------------------------------------------------
# Health / status
# ---------------------------------------------------------------------------

@router.get("/health", response_model=HealthResponse, tags=["health"])
def health_check():
    """
    Enhanced health check endpoint.
    
    Returns:
        - status: Service health status
        - service: Service name
        - scheduler_running: Whether background scheduler is active
        - erp_mode: Current ERP adapter mode (mock/sap)
    """
    return HealthResponse(
        status="ok",
        service="erp-approval-middleware",
        scheduler_running=is_scheduler_running(),
        erp_mode=settings.erp_mode,
    )


# ---------------------------------------------------------------------------
# Detection & Staging
# ---------------------------------------------------------------------------

@router.post("/detect", response_model=DetectResponse, tags=["approvals"])
def detect_requisitions(db: Session = Depends(get_db), _user: dict = Depends(get_current_user)):
    """
    Trigger detection and staging of pending requisitions from ERP.
    
    This endpoint:
    1. Fetches pending requisitions from the ERP system
    2. Computes risk scores for each requisition
    3. Makes approval decisions based on risk thresholds
    4. Stages decisions in the database with grace period
    
    Returns:
        Number of decisions staged and optional message.
        
    Raises:
        HTTPException: 503 if ERP adapter is not available
    """
    logger.info("POST /detect - triggering requisition detection")
    
    # Get ERP adapter from scheduler
    adapter = get_adapter()
    if not adapter:
        logger.error("ERP adapter not available")
        raise HTTPException(
            status_code=503,
            detail="ERP adapter not initialized. Ensure scheduler is running.",
        )
    
    # Ensure adapter is connected
    try:
        adapter.connect()
    except Exception as exc:
        logger.warning("Adapter connection attempt: %s", exc)
    
    # Create orchestrator and trigger detection
    orchestrator = ApprovalOrchestrator()
    
    try:
        staged_decisions = orchestrator.detect_and_stage_requisitions(
            adapter=adapter,
            db=db,
        )
        
        staged_count = len(staged_decisions)
        logger.info("Successfully staged %d decisions", staged_count)
        
        return DetectResponse(
            staged_count=staged_count,
            message=f"Staged {staged_count} approval decision(s)" if staged_count > 0 else "No pending requisitions found",
        )
        
    except Exception as exc:
        logger.error("Detection failed: %s", exc)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to detect requisitions: {str(exc)}",
        )


# ---------------------------------------------------------------------------
# Undo / Cancel
# ---------------------------------------------------------------------------

@router.post("/undo/{erp_requisition_id}", response_model=UndoResponse, tags=["approvals"])
def undo_decision(erp_requisition_id: str, db: Session = Depends(get_db), _user: dict = Depends(get_current_user)):
    """
    Cancel a pending approval decision (grace period undo).
    
    This allows users to cancel an auto-approval before it's committed
    to the ERP system. Only works for decisions in 'pending_commit' state.
    
    Args:
        erp_requisition_id: ERP requisition identifier
        
    Returns:
        Updated decision state and confirmation message.
        
    Raises:
        HTTPException: 404 if no pending decision found
    """
    logger.info("POST /undo/%s - attempting to cancel decision", erp_requisition_id)
    
    orchestrator = ApprovalOrchestrator()
    
    try:
        cancelled_decision = orchestrator.undo_decision(
            erp_requisition_id=erp_requisition_id,
            db=db,
        )
        
        if not cancelled_decision:
            logger.warning("No pending decision found for %s", erp_requisition_id)
            raise HTTPException(
                status_code=404,
                detail=f"No pending decision found for requisition {erp_requisition_id}",
            )
        
        logger.info("Successfully cancelled decision for %s", erp_requisition_id)
        
        return UndoResponse(
            erp_requisition_id=erp_requisition_id,
            state=cancelled_decision.state,
            message=f"Decision cancelled successfully",
        )
        
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Undo failed: %s", exc)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to cancel decision: {str(exc)}",
        )


# ---------------------------------------------------------------------------
# Manual Approve / Reject
# ---------------------------------------------------------------------------

@router.post("/approve/{erp_requisition_id}", response_model=ApproveRejectResponse, tags=["approvals"])
def approve_decision(
    erp_requisition_id: str,
    body: ApproveRejectRequest | None = None,
    db: Session = Depends(get_db),
    _user: dict = Depends(get_current_user),
):
    """
    Manually approve a pending requisition.

    One-click approve — transitions the decision from pending_commit to
    committed with decision = manual_approve.  Accepts an optional comment.

    Args:
        erp_requisition_id: ERP requisition identifier
        body: Optional request body containing a comment

    Raises:
        HTTPException: 404 if no pending decision found
    """
    logger.info("POST /approve/%s", erp_requisition_id)
    orchestrator = ApprovalOrchestrator()

    try:
        comment = body.comment if body else None
        result = orchestrator.approve_decision(
            erp_requisition_id=erp_requisition_id,
            db=db,
            comment=comment,
        )

        if not result:
            raise HTTPException(
                status_code=404,
                detail=f"No pending decision found for requisition {erp_requisition_id}",
            )

        return ApproveRejectResponse(
            erp_requisition_id=erp_requisition_id,
            decision=result.decision,
            state=result.state,
            message="Decision approved successfully",
        )

    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Approve failed: %s", exc)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to approve decision: {str(exc)}",
        )


@router.post("/reject/{erp_requisition_id}", response_model=ApproveRejectResponse, tags=["approvals"])
def reject_decision(
    erp_requisition_id: str,
    body: ApproveRejectRequest | None = None,
    db: Session = Depends(get_db),
    _user: dict = Depends(get_current_user),
):
    """
    Manually reject a pending requisition.

    Transitions the decision from pending_commit to committed with
    decision = reject.  Accepts an optional rejection reason.

    Args:
        erp_requisition_id: ERP requisition identifier
        body: Optional request body containing a rejection reason

    Raises:
        HTTPException: 404 if no pending decision found
    """
    logger.info("POST /reject/%s", erp_requisition_id)
    orchestrator = ApprovalOrchestrator()

    try:
        comment = body.comment if body else None
        result = orchestrator.reject_decision(
            erp_requisition_id=erp_requisition_id,
            db=db,
            comment=comment,
        )

        if not result:
            raise HTTPException(
                status_code=404,
                detail=f"No pending decision found for requisition {erp_requisition_id}",
            )

        return ApproveRejectResponse(
            erp_requisition_id=erp_requisition_id,
            decision=result.decision,
            state=result.state,
            message="Decision rejected successfully",
        )

    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Reject failed: %s", exc)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to reject decision: {str(exc)}",
        )


# ---------------------------------------------------------------------------
# Decisions Query
# ---------------------------------------------------------------------------

@router.get("/decisions", response_model=DecisionListResponse, tags=["approvals"])
def list_decisions(
    state: Optional[str] = Query(None, description="Filter by state (e.g., pending_commit, committed, cancelled)"),
    db: Session = Depends(get_db),
    _user: dict = Depends(get_current_user),
):
    """
    List all approval decisions with optional state filtering.
    
    Query parameters:
        - state: Optional filter by decision state
          Valid states: detected, pending_commit, committed, cancelled, failed
          
    Returns:
        List of approval decisions and total count.
    """
    logger.info("GET /decisions - state filter: %s", state)
    
    try:
        # Build query
        stmt = select(ApprovalDecision).order_by(ApprovalDecision.created_at.desc())
        
        # Apply state filter if provided
        if state:
            stmt = stmt.where(ApprovalDecision.state == state)
            logger.debug("Filtering decisions by state: %s", state)
        
        # Execute query
        decisions = db.execute(stmt).scalars().all()
        
        # Convert to response models (UUID to string)
        decisions_out = [
            ApprovalDecisionOut(
                id=str(decision.id),
                erp_requisition_id=decision.erp_requisition_id,
                risk_score=decision.risk_score,
                risk_explanation=decision.risk_explanation,
                decision=decision.decision,
                state=decision.state,
                commit_at=decision.commit_at,
                committed_at=decision.committed_at,
                comment=decision.comment,
                created_at=decision.created_at,
            )
            for decision in decisions
        ]
        
        logger.info("Returning %d decisions", len(decisions_out))
        
        return DecisionListResponse(
            decisions=decisions_out,
            total=len(decisions_out),
        )
        
    except Exception as exc:
        logger.error("Failed to query decisions: %s", exc)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to retrieve decisions: {str(exc)}",
        )


# ---------------------------------------------------------------------------
# Notifications Query
# ---------------------------------------------------------------------------

@router.get("/notifications", response_model=NotificationListResponse, tags=["notifications"])
def list_notifications(
    channel: Optional[str] = Query(None, description="Filter by channel (email, slack)"),
    erp_requisition_id: Optional[str] = Query(None, description="Filter by requisition ID"),
    db: Session = Depends(get_db),
    _user: dict = Depends(get_current_user),
):
    """
    List all notification logs with optional filtering.

    Query parameters:
        - channel: Filter by notification channel (email | slack)
        - erp_requisition_id: Filter by ERP requisition ID

    Returns:
        List of notification log records and total count.
    """
    logger.info(
        "GET /notifications - channel=%s, erp_requisition_id=%s",
        channel,
        erp_requisition_id,
    )

    try:
        stmt = select(NotificationLog).order_by(NotificationLog.created_at.desc())

        if channel:
            stmt = stmt.where(NotificationLog.channel == channel)
        if erp_requisition_id:
            stmt = stmt.where(
                NotificationLog.erp_requisition_id == erp_requisition_id
            )

        notifications = db.execute(stmt).scalars().all()

        notifications_out = [
            NotificationLogOut.model_validate(n)
            for n in notifications
        ]

        logger.info("Returning %d notifications", len(notifications_out))

        return NotificationListResponse(
            notifications=notifications_out,
            total=len(notifications_out),
        )

    except Exception as exc:
        logger.error("Failed to query notifications: %s", exc)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to retrieve notifications: {str(exc)}",
        )


# ---------------------------------------------------------------------------
# Adapter Management
# ---------------------------------------------------------------------------

@router.post("/switch-adapter", tags=["admin"])
def switch_adapter(mode: str = Query(..., regex="^(mock|sap|hybrid)$"), _user: dict = Depends(get_current_user)):
    """
    Switch the ERP adapter at runtime between mock and SAP.

    Args:
        mode: "mock" or "sap"

    Returns:
        New adapter status including type and health check.
    """
    logger.info("POST /switch-adapter — switching to %s", mode)

    try:
        new_adapter = set_adapter(mode)
        health = new_adapter.health_check()

        return {
            "status": "ok",
            "adapter": mode,
            "message": f"Switched to {mode} adapter",
            "health": health,
        }

    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))

    except Exception as exc:
        logger.error("Adapter switch failed: %s", exc)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to switch adapter: {str(exc)}",
        )
