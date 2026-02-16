"""
Pydantic schemas — API request / response models.

These are the EXTERNAL contract (what the REST API sends and receives).
Keep them separate from internal domain models and ORM models.
"""

from datetime import datetime
from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------

class HealthResponse(BaseModel):
    """Response for GET /health."""
    status: str = Field(..., examples=["ok"])
    service: str = Field(default="erp-approval-middleware", examples=["erp-approval-middleware"])
    scheduler_running: bool = Field(default=False)
    erp_mode: str = Field(..., examples=["mock", "sap"])


# ---------------------------------------------------------------------------
# Requisitions
# ---------------------------------------------------------------------------

class RequisitionOut(BaseModel):
    """Serialised requisition returned by the API."""
    erp_requisition_id: str
    item_number: str | None = None
    material: str | None = None
    description: str | None = None
    quantity: float | None = None
    unit: str | None = None
    price: float | None = None
    currency: str | None = None
    plant: str | None = None
    fetched_at: datetime | None = None

    model_config = {"from_attributes": True}


class SyncResponse(BaseModel):
    """Response for POST /requisitions/sync."""
    synced: int
    message: str | None = None


# ---------------------------------------------------------------------------
# Decisions
# ---------------------------------------------------------------------------

class DecisionOut(BaseModel):
    """Serialised approval decision returned by the API."""
    erp_requisition_id: str
    risk_score: float | None = None
    risk_explanation: str | None = None
    decision: str
    reason: str | None = None
    committed: bool = False
    created_at: datetime | None = None

    model_config = {"from_attributes": True}


class CommitResponse(BaseModel):
    """Response for POST /decisions/commit."""
    committed: int
    message: str | None = None


class ApprovalDecisionOut(BaseModel):
    """Serialised approval decision returned by the API."""
    id: str
    erp_requisition_id: str
    risk_score: float | None = None
    risk_explanation: str | None = None
    decision: str
    state: str
    commit_at: datetime
    committed_at: datetime | None = None
    comment: str | None = None
    created_at: datetime

    model_config = {"from_attributes": True}


class DecisionListResponse(BaseModel):
    """Response for GET /decisions."""
    decisions: list[ApprovalDecisionOut]
    total: int


class DetectResponse(BaseModel):
    """Response for POST /detect."""
    staged_count: int
    message: str | None = None


class UndoResponse(BaseModel):
    """Response for POST /undo/{erp_requisition_id}."""
    erp_requisition_id: str
    state: str
    message: str


class ApproveRejectRequest(BaseModel):
    """Request body for POST /approve and /reject."""
    comment: str | None = Field(
        default=None,
        max_length=500,
        examples=["Verified with vendor — pricing is correct"],
        description="Optional comment or justification for the decision",
    )


class ApproveRejectResponse(BaseModel):
    """Response for POST /approve and /reject."""
    erp_requisition_id: str
    decision: str
    state: str
    message: str


# ---------------------------------------------------------------------------
# Notifications
# ---------------------------------------------------------------------------

class NotificationLogOut(BaseModel):
    """Serialised notification log returned by the API."""
    id: int
    erp_requisition_id: str
    decision: str
    channel: str
    status: str
    message: str | None = None
    created_at: datetime

    model_config = {"from_attributes": True}


class NotificationListResponse(BaseModel):
    """Response for GET /notifications."""
    notifications: list[NotificationLogOut]
    total: int
