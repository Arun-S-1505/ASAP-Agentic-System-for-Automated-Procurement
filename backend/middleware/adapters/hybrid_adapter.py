"""
HybridERPAdapter — Reads from SAP API Hub Sandbox, simulates writes locally.

This adapter combines the best of both worlds for demo/research purposes:
    - fetch_pending_requisitions()  → Real SAP OData API (read-only sandbox)
    - get_requisition_details()     → Real SAP OData API
    - submit_approval()             → Simulated locally (sandbox is read-only)
    - health_check()                → Checks SAP sandbox connectivity

This lets the middleware display genuine SAP Purchase Requisition data
while still supporting the full approval workflow end-to-end.
"""

import logging
from typing import Any

from .sap_adapter import SAPERPAdapter
from models.domain import RequisitionDTO

logger = logging.getLogger(__name__)


class HybridERPAdapter(SAPERPAdapter):
    """
    Extends SAPERPAdapter with simulated write operations.

    Reads go to the real SAP API Hub Sandbox.
    Writes (approve/reject) are simulated locally since the sandbox
    does not support POST/PATCH operations.
    """

    # ------------------------------------------------------------------
    # Override: Fetch (remove release-code filter for sandbox)
    # ------------------------------------------------------------------

    def fetch_pending_requisitions(
        self, **filters,
    ) -> list[RequisitionDTO]:
        """
        Fetch top 10 requisitions from SAP Sandbox.

        The sandbox has pre-loaded sample data that doesn't match the
        on-premise PurchaseRequisitionReleaseCode filter. We override
        the default filter with a no-op to fetch all available items,
        limited to 10 for the dashboard.
        """
        # Override filter to fetch all items from sandbox
        filters.setdefault("filter", "PurchaseRequisition ne ''")
        filters.setdefault("top", "10")
        return super().fetch_pending_requisitions(**filters)

    # ------------------------------------------------------------------
    # Override: Approval Operations (simulated)
    # ------------------------------------------------------------------

    def submit_approval(
        self,
        requisition_id: str,
        decision: str,
        comment: str = "",
    ) -> dict[str, Any]:
        """
        Simulate an approval/rejection decision.

        The SAP API Hub Sandbox is read-only, so write operations are
        simulated locally. The response mimics what a real SAP system
        would return.

        Args:
            requisition_id: SAP purchase requisition number.
            decision: One of: approve, auto_approve, manual_approve,
                      reject, hold, cancel.
            comment: Optional reason text.

        Returns:
            Simulated confirmation payload.
        """
        logger.info(
            "HybridERPAdapter: SIMULATED %s for %s (sandbox is read-only)",
            decision,
            requisition_id,
        )

        return {
            "status": "ok",
            "requisition_id": requisition_id,
            "decision": decision,
            "simulated": True,
            "message": (
                f"Decision '{decision}' for {requisition_id} simulated "
                f"(SAP Sandbox is read-only). In production, this would "
                f"call PurchaseRequisitionRelease OData action."
            ),
        }

    # ------------------------------------------------------------------
    # Override: Health Check (adds hybrid indicator)
    # ------------------------------------------------------------------

    def health_check(self) -> dict[str, Any]:
        """
        Health check that indicates hybrid mode.

        Calls the parent SAP health check and adds a flag showing
        that writes are simulated.
        """
        health = super().health_check()
        health["adapter"] = "hybrid"
        health["writes_simulated"] = True
        return health
