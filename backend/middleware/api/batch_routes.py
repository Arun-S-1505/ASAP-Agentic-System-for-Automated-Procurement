"""
Batch operations — approve or reject multiple decisions at once.

Provides:
    POST /batch/approve — bulk-approve a list of requisition IDs
    POST /batch/reject  — bulk-reject  a list of requisition IDs
"""

import logging

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session

from core.auth import get_current_user
from db.session import get_db
from services.orchestrator import ApprovalOrchestrator

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/batch", tags=["batch"])


# ---------------------------------------------------------------------------
# Request / Response schemas
# ---------------------------------------------------------------------------

class BatchRequest(BaseModel):
    ids: list[str]          # erp_requisition_id values
    comment: str | None = None


class BatchItemResult(BaseModel):
    erp_requisition_id: str
    success: bool
    message: str


class BatchResponse(BaseModel):
    processed: int
    failed: int
    results: list[BatchItemResult]


# ---------------------------------------------------------------------------
# POST /batch/approve
# ---------------------------------------------------------------------------

@router.post("/approve", response_model=BatchResponse)
def batch_approve(
    body: BatchRequest,
    db: Session = Depends(get_db),
    _user: dict = Depends(get_current_user),
):
    """Approve multiple pending requisitions in one call."""
    logger.info("POST /batch/approve — %d items", len(body.ids))

    if not body.ids:
        raise HTTPException(status_code=400, detail="ids list cannot be empty")

    orchestrator = ApprovalOrchestrator()
    results: list[BatchItemResult] = []
    failed = 0

    for erp_id in body.ids:
        try:
            result = orchestrator.approve_decision(
                erp_requisition_id=erp_id,
                db=db,
                comment=body.comment,
            )
            if result:
                results.append(BatchItemResult(
                    erp_requisition_id=erp_id,
                    success=True,
                    message="Approved",
                ))
            else:
                failed += 1
                results.append(BatchItemResult(
                    erp_requisition_id=erp_id,
                    success=False,
                    message="No pending decision found",
                ))
        except Exception as exc:
            failed += 1
            results.append(BatchItemResult(
                erp_requisition_id=erp_id,
                success=False,
                message=str(exc),
            ))

    return BatchResponse(
        processed=len(body.ids) - failed,
        failed=failed,
        results=results,
    )


# ---------------------------------------------------------------------------
# POST /batch/reject
# ---------------------------------------------------------------------------

@router.post("/reject", response_model=BatchResponse)
def batch_reject(
    body: BatchRequest,
    db: Session = Depends(get_db),
    _user: dict = Depends(get_current_user),
):
    """Reject multiple pending requisitions in one call."""
    logger.info("POST /batch/reject — %d items", len(body.ids))

    if not body.ids:
        raise HTTPException(status_code=400, detail="ids list cannot be empty")

    orchestrator = ApprovalOrchestrator()
    results: list[BatchItemResult] = []
    failed = 0

    for erp_id in body.ids:
        try:
            result = orchestrator.reject_decision(
                erp_requisition_id=erp_id,
                db=db,
                comment=body.comment,
            )
            if result:
                results.append(BatchItemResult(
                    erp_requisition_id=erp_id,
                    success=True,
                    message="Rejected",
                ))
            else:
                failed += 1
                results.append(BatchItemResult(
                    erp_requisition_id=erp_id,
                    success=False,
                    message="No pending decision found",
                ))
        except Exception as exc:
            failed += 1
            results.append(BatchItemResult(
                erp_requisition_id=erp_id,
                success=False,
                message=str(exc),
            ))

    return BatchResponse(
        processed=len(body.ids) - failed,
        failed=failed,
        results=results,
    )
