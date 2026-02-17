"""
SQLAlchemy ORM models — database table definitions.

All models inherit from a shared declarative Base so Alembic migrations
can discover them automatically.

Using SQLAlchemy 2.0+ style with mapped_column.
"""

import uuid
from datetime import datetime

from sqlalchemy import String, Float, DateTime, Text, Index
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.sql import func


class Base(DeclarativeBase):
    """Base class for all ORM models."""
    pass


# ---------------------------------------------------------------------------
# Requisition snapshot — cached copy of ERP data
# ---------------------------------------------------------------------------
class Requisition(Base):
    """
    Local cache of a purchase requisition fetched from the ERP.
    
    This table stores a snapshot of requisition data for audit and
    offline processing purposes.
    """

    __tablename__ = "requisitions"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    erp_requisition_id: Mapped[str] = mapped_column(
        String(50), unique=True, nullable=False, index=True
    )
    item_number: Mapped[str | None] = mapped_column(String(20))
    material: Mapped[str | None] = mapped_column(String(100))
    description: Mapped[str | None] = mapped_column(Text)
    quantity: Mapped[float | None] = mapped_column(Float)
    unit: Mapped[str | None] = mapped_column(String(10))
    price: Mapped[float | None] = mapped_column(Float)
    currency: Mapped[str | None] = mapped_column(String(5))
    plant: Mapped[str | None] = mapped_column(String(20))

    fetched_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now()
    )

    def __repr__(self) -> str:
        return f"<Requisition {self.erp_requisition_id}>"


# ---------------------------------------------------------------------------
# Approval decision — grace-buffer approval system
# ---------------------------------------------------------------------------
class ApprovalDecision(Base):
    """
    Stores approval decisions with grace-buffer before ERP commit.
    
    State Machine:
        detected → pending_commit → committed
                ↓                 ↓
             cancelled         failed
    
    Fields:
        - id: UUID primary key
        - erp_requisition_id: Reference to ERP requisition
        - risk_score: Computed risk value (0-100)
        - decision: Approval outcome
        - state: Current lifecycle state
        - commit_at: When to commit to ERP (grace period end)
        - comment: Optional explanation text
        - error_message: Populated if commit fails
        - committed_at: Actual commit timestamp
        - created_at: Detection timestamp
        - updated_at: Last modification timestamp
    """

    __tablename__ = "approval_decisions"

    # Primary key: UUID for distributed system compatibility  
    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
        nullable=False,
    )

    # ERP reference (indexed for lookups)
    erp_requisition_id: Mapped[str] = mapped_column(
        String(50), nullable=False, index=True
    )

    # Risk assessment
    risk_score: Mapped[float | None] = mapped_column(Float)
    risk_explanation: Mapped[str | None] = mapped_column(
        Text, comment="Human-readable, rule-based risk explanation"
    )

    # Decision outcome
    decision: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="hold",
        comment="auto_approve | manual_approve | reject | hold",
    )

    # Lifecycle state (indexed for efficient querying pending commits)
    state: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="detected",
        index=True,
        comment="detected | pending_commit | cancelled | committed | failed",
    )

    # Grace period timing (indexed for scheduler queries)
    commit_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, index=True
    )

    # Optional explanation
    comment: Mapped[str | None] = mapped_column(Text)

    # --- Enriched SAP requisition fields ---
    product_name: Mapped[str | None] = mapped_column(String(255))
    material_code: Mapped[str | None] = mapped_column(String(100))
    material_group: Mapped[str | None] = mapped_column(String(50))
    quantity: Mapped[float | None] = mapped_column(Float)
    unit: Mapped[str | None] = mapped_column(String(10))
    unit_price: Mapped[float | None] = mapped_column(Float)
    total_amount: Mapped[float | None] = mapped_column(Float)
    currency: Mapped[str | None] = mapped_column(String(5))
    plant: Mapped[str | None] = mapped_column(String(20))
    company_code: Mapped[str | None] = mapped_column(String(20))
    purchasing_group: Mapped[str | None] = mapped_column(String(20))
    created_by: Mapped[str | None] = mapped_column(String(100))
    supplier: Mapped[str | None] = mapped_column(String(50))
    release_status: Mapped[str | None] = mapped_column(String(10))
    processing_status: Mapped[str | None] = mapped_column(String(10))
    is_deleted: Mapped[bool | None] = mapped_column(default=False)
    is_closed: Mapped[bool | None] = mapped_column(default=False)
    creation_date: Mapped[str | None] = mapped_column(String(30))
    delivery_date: Mapped[str | None] = mapped_column(String(30))

    # Commit tracking
    committed_at: Mapped[datetime | None] = mapped_column(DateTime)
    error_message: Mapped[str | None] = mapped_column(Text)

    # Audit timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime,
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    def __repr__(self) -> str:
        return (
            f"<ApprovalDecision {self.erp_requisition_id} "
            f"decision={self.decision} state={self.state}>"
        )


# ---------------------------------------------------------------------------
# Simulated ERP requisition state — used by MockERPAdapter
# ---------------------------------------------------------------------------
class ERPSimulatedRequisition(Base):
    """
    Simulated ERP requisition state — used by MockERPAdapter.

    This table mimics a real ERP system's internal purchase-requisition
    table.  MockERPAdapter reads and writes here instead of keeping
    data in memory, so the middleware can be restarted without losing
    simulated ERP state.

    Status lifecycle:
        pending → approved   (via submit_approval with decision=approve)
        pending → rejected   (via submit_approval with decision=reject)
        pending → cancelled  (via submit_approval with decision=cancel/hold)
    """

    __tablename__ = "erp_simulated_requisitions"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)

    erp_requisition_id: Mapped[str] = mapped_column(
        String(50), unique=True, nullable=False, index=True,
    )

    # Requisition detail fields (mirror of RequisitionDTO)
    item_number: Mapped[str | None] = mapped_column(String(20))
    material: Mapped[str | None] = mapped_column(String(100))
    description: Mapped[str | None] = mapped_column(Text)
    quantity: Mapped[float | None] = mapped_column(Float)
    unit: Mapped[str | None] = mapped_column(String(10))
    price: Mapped[float | None] = mapped_column(Float)
    currency: Mapped[str | None] = mapped_column(String(5))
    plant: Mapped[str | None] = mapped_column(String(20))

    # ERP status tracking
    status: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="pending",
        index=True,
        comment="pending | approved | rejected | cancelled",
    )

    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False,
    )
    last_updated_at: Mapped[datetime] = mapped_column(
        DateTime,
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    def __repr__(self) -> str:
        return (
            f"<ERPSimulatedRequisition {self.erp_requisition_id} "
            f"status={self.status}>"
        )


# ---------------------------------------------------------------------------
# Notification log — simulated enterprise notifications
# ---------------------------------------------------------------------------
class NotificationLog(Base):
    """
    Stores simulated notification records sent after ERP commit.

    Each committed decision can generate multiple notifications
    (one per channel: email, Slack, etc.).
    """

    __tablename__ = "notification_logs"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)

    erp_requisition_id: Mapped[str] = mapped_column(
        String(50), nullable=False, index=True
    )

    decision: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        comment="auto_approve | manual_approve | reject | hold",
    )

    channel: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        comment="email | slack",
    )

    status: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="sent",
        comment="sent | failed",
    )

    message: Mapped[str | None] = mapped_column(Text)

    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False
    )

    def __repr__(self) -> str:
        return (
            f"<NotificationLog {self.erp_requisition_id} "
            f"channel={self.channel} status={self.status}>"
        )


# ---------------------------------------------------------------------------
# Composite indexes for query optimization
# ---------------------------------------------------------------------------
# Index for finding pending commits that are ready
Index(
    "ix_approval_decision_pending_commits",
    ApprovalDecision.state,
    ApprovalDecision.commit_at,
)
