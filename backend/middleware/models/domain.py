"""
Internal domain models — framework-agnostic data transfer objects.

These represent the canonical shape of data INSIDE the middleware.
They decouple the core engine from both Pydantic (API) and SQLAlchemy (DB).
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional


@dataclass
class RequisitionDTO:
    """
    Normalised purchase requisition — adapter output / core engine input.

    Adapters convert ERP-specific formats into this DTO before
    handing data to the core engine.
    """
    erp_requisition_id: str
    item_number: Optional[str] = None
    material: Optional[str] = None
    description: Optional[str] = None
    quantity: Optional[float] = None
    unit: Optional[str] = None
    price: Optional[float] = None
    currency: Optional[str] = None
    plant: Optional[str] = None
    fetched_at: Optional[datetime] = None


@dataclass
class DecisionDTO:
    """
    Approval decision — core engine output / persistence input.
    """
    erp_requisition_id: str
    risk_score: float = 0.0
    decision: str = "hold"          # auto_approve | escalate | reject | hold
    reason: Optional[str] = None
    risk_explanation: Optional[str] = None
    committed: bool = False
    created_at: Optional[datetime] = field(default_factory=datetime.utcnow)
