"""
ERPAdapter — Abstract base class for ERP integrations.

Every ERP connector (SAP, Oracle, Mock, etc.) must implement this interface.
The core engine and services layer depend ONLY on this abstraction.
"""

from abc import ABC, abstractmethod
from typing import Any

from models.domain import RequisitionDTO


class ERPAdapter(ABC):
    """
    Port interface for ERP system communication.

    Methods cover the minimum operations the middleware needs:
    - Fetch pending purchase requisitions
    - Submit approval / rejection decisions
    - Query requisition status
    """

    # ------------------------------------------------------------------
    # Lifecycle
    # ------------------------------------------------------------------

    @abstractmethod
    def connect(self) -> None:
        """Establish connection to the ERP system."""
        ...

    @abstractmethod
    def disconnect(self) -> None:
        """Clean up resources and close connection."""
        ...

    # ------------------------------------------------------------------
    # Purchase Requisition Operations
    # ------------------------------------------------------------------

    @abstractmethod
    def fetch_pending_requisitions(self, **filters: Any) -> list[RequisitionDTO]:
        """
        Retrieve purchase requisitions awaiting approval.

        Returns:
            List of normalized RequisitionDTO instances.
        """
        ...

    @abstractmethod
    def get_requisition_details(self, requisition_id: str) -> RequisitionDTO:
        """
        Fetch full details for a single requisition.

        Args:
            requisition_id: Unique identifier in the ERP system.

        Returns:
            Normalized RequisitionDTO instance.
        """
        ...

    # ------------------------------------------------------------------
    # Approval Operations
    # ------------------------------------------------------------------

    @abstractmethod
    def submit_approval(self, requisition_id: str, decision: str, comment: str = "") -> dict[str, Any]:
        """
        Push an approval / rejection decision back to the ERP.

        Args:
            requisition_id: Target requisition.
            decision: "approve" | "reject" | "hold".
            comment: Optional reason text.

        Returns:
            Confirmation payload from the ERP.
        """
        ...

    # ------------------------------------------------------------------
    # Health
    # ------------------------------------------------------------------

    @abstractmethod
    def health_check(self) -> dict[str, Any]:
        """Return connectivity / health status."""
        ...
