"""
MockERPAdapter — MySQL-backed stateful fake ERP for development and testing.

Instead of returning hard-coded in-memory data, this adapter persists
simulated requisitions in the ``erp_simulated_requisitions`` table so
that state survives server restarts and the full approve / reject /
cancel lifecycle is observable.

Design decisions:
    * ``connect()`` seeds sample rows when the table is empty (first run).
    * ``fetch_pending_requisitions()`` reads rows with status = "pending".
    * ``submit_approval()`` transitions the row to approved / rejected /
      cancelled and returns a confirmation payload.
    * Every write is flushed in its own short-lived session to avoid
      leaking transactions.
"""

import logging
from datetime import datetime
from typing import Any

from sqlalchemy import select, func as sa_func
from sqlalchemy.orm import Session

from .base import ERPAdapter
from db.models import ERPSimulatedRequisition
from db.session import SessionLocal
from models.domain import RequisitionDTO

logger = logging.getLogger(__name__)

# Mapping from middleware decision vocabulary → simulated ERP status
_DECISION_STATUS_MAP: dict[str, str] = {
    "approve": "approved",
    "auto_approve": "approved",
    "manual_approve": "approved",
    "reject": "rejected",
    "cancel": "cancelled",
    "hold": "cancelled",
}

# Valid terminal statuses (cannot be changed once reached)
_TERMINAL_STATUSES = frozenset({"approved", "rejected", "cancelled"})


class MockERPAdapter(ERPAdapter):
    """
    MySQL-backed mock ERP adapter.

    All reads and writes go through the ``erp_simulated_requisitions`` table
    using short-lived ``SessionLocal()`` sessions so there is no coupling to
    the FastAPI request-scoped session.
    """

    # ------------------------------------------------------------------
    # Sample seed data (inserted on first connect when table is empty)
    # ------------------------------------------------------------------
    _SEED_DATA: list[dict[str, Any]] = [
        {
            "erp_requisition_id": "PR-2026-001",
            "item_number": "00010",
            "material": "MAT-1001",
            "description": "Laptop Computer — Dell XPS 15",
            "quantity": 5.0,
            "unit": "EA",
            "price": 1500.00,
            "currency": "USD",
            "plant": "PLANT-US-001",
        },
        {
            "erp_requisition_id": "PR-2026-002",
            "item_number": "00010",
            "material": "MAT-2050",
            "description": "Office Furniture — Standing Desk",
            "quantity": 10.0,
            "unit": "EA",
            "price": 800.00,
            "currency": "USD",
            "plant": "PLANT-US-001",
        },
        {
            "erp_requisition_id": "PR-2026-003",
            "item_number": "00010",
            "material": "MAT-3020",
            "description": "Industrial Equipment — CNC Machine",
            "quantity": 2.0,
            "unit": "EA",
            "price": 45000.00,
            "currency": "USD",
            "plant": "PLANT-DE-001",
        },
        {
            "erp_requisition_id": "PR-2026-004",
            "item_number": "00010",
            "material": "MAT-4010",
            "description": "Server Rack — 42U Cabinet",
            "quantity": 3.0,
            "unit": "EA",
            "price": 2200.00,
            "currency": "USD",
            "plant": "PLANT-US-002",
        },
        {
            "erp_requisition_id": "PR-2026-005",
            "item_number": "00010",
            "material": "MAT-5005",
            "description": "Safety Equipment — Fire Suppression System",
            "quantity": 1.0,
            "unit": "EA",
            "price": 18500.00,
            "currency": "EUR",
            "plant": "PLANT-DE-001",
        },
    ]

    def __init__(self) -> None:
        self._connected: bool = False

    # ------------------------------------------------------------------
    # Lifecycle
    # ------------------------------------------------------------------

    def connect(self) -> None:
        """
        Mark as connected and seed the simulated ERP table if it is empty.
        """
        if self._connected:
            return

        self._seed_if_empty()
        self._connected = True
        logger.info("MockERPAdapter connected (MySQL-backed, stateful)")

    def disconnect(self) -> None:
        logger.info("MockERPAdapter disconnected")
        self._connected = False

    # ------------------------------------------------------------------
    # Purchase Requisition Operations
    # ------------------------------------------------------------------

    def fetch_pending_requisitions(self, **filters: Any) -> list[RequisitionDTO]:
        """
        Return requisitions with ``status = 'pending'`` from the simulated
        ERP table.  Filters are accepted but currently ignored.
        """
        logger.debug(
            "MockERPAdapter.fetch_pending_requisitions(filters=%s)", filters,
        )

        with SessionLocal() as db:
            stmt = (
                select(ERPSimulatedRequisition)
                .where(ERPSimulatedRequisition.status == "pending")
                .order_by(ERPSimulatedRequisition.created_at)
            )
            rows: list[ERPSimulatedRequisition] = list(
                db.execute(stmt).scalars().all()
            )

        dtos = [self._row_to_dto(row) for row in rows]
        logger.info(
            "MockERPAdapter: fetched %d pending requisitions", len(dtos),
        )
        return dtos

    def get_requisition_details(self, requisition_id: str) -> RequisitionDTO:
        """
        Look up a single requisition by ``erp_requisition_id``.

        If the ID does not exist in the simulated table a new *pending*
        row is auto-created (mirrors an ERP that always has the record).
        """
        logger.debug(
            "MockERPAdapter.get_requisition_details(%s)", requisition_id,
        )

        with SessionLocal() as db:
            row = self._get_or_create(requisition_id, db)
            db.commit()
            return self._row_to_dto(row)

    # ------------------------------------------------------------------
    # Approval Operations
    # ------------------------------------------------------------------

    def submit_approval(
        self,
        requisition_id: str,
        decision: str,
        comment: str = "",
    ) -> dict[str, Any]:
        """
        Transition the simulated requisition to its new ERP status.

        Args:
            requisition_id: ERP requisition identifier.
            decision: One of ``approve``, ``auto_approve``,
                      ``manual_approve``, ``reject``, ``hold`` / ``cancel``.
            comment: Optional reason text.

        Returns:
            Confirmation payload including ``status`` (``"simulated"``),
            ``previous_status`` and ``new_status``.

        Raises:
            ValueError: If the requisition is already in a terminal state.
        """
        new_status = _DECISION_STATUS_MAP.get(decision)
        if new_status is None:
            logger.warning(
                "MockERPAdapter: unknown decision '%s', defaulting to cancelled",
                decision,
            )
            new_status = "cancelled"

        with SessionLocal() as db:
            row = self._get_or_create(requisition_id, db)
            previous_status = row.status

            # Guard: prevent double-processing of terminal states
            if previous_status in _TERMINAL_STATUSES:
                logger.warning(
                    "MockERPAdapter: requisition %s already in terminal state '%s' — cannot transition to '%s'",
                    requisition_id,
                    previous_status,
                    new_status,
                )
                return {
                    "status": "already_processed",
                    "requisition_id": requisition_id,
                    "current_status": previous_status,
                    "requested_decision": decision,
                    "message": f"Requisition already in terminal state '{previous_status}'",
                }

            # Perform the state transition
            row.status = new_status
            row.last_updated_at = datetime.utcnow()
            db.commit()

            logger.info(
                "MockERPAdapter: requisition %s transitioned %s → %s (decision=%s, comment=%s)",
                requisition_id,
                previous_status,
                new_status,
                decision,
                comment or "(none)",
            )

        return {
            "status": "simulated",
            "requisition_id": requisition_id,
            "decision": decision,
            "previous_status": previous_status,
            "new_status": new_status,
        }

    # ------------------------------------------------------------------
    # Health
    # ------------------------------------------------------------------

    def health_check(self) -> dict[str, Any]:
        """
        Verify MySQL connectivity and return row counts.
        """
        try:
            with SessionLocal() as db:
                total = db.execute(
                    select(sa_func.count()).select_from(ERPSimulatedRequisition)
                ).scalar() or 0
                pending = db.execute(
                    select(sa_func.count())
                    .select_from(ERPSimulatedRequisition)
                    .where(ERPSimulatedRequisition.status == "pending")
                ).scalar() or 0

            return {
                "status": "healthy",
                "adapter": "mock",
                "connected": self._connected,
                "backend": "mysql",
                "simulated_requisitions_total": total,
                "simulated_requisitions_pending": pending,
            }
        except Exception as exc:
            logger.error("MockERPAdapter health check failed: %s", exc)
            return {
                "status": "unhealthy",
                "adapter": "mock",
                "error": str(exc),
            }

    # ------------------------------------------------------------------
    # Private helpers
    # ------------------------------------------------------------------

    def _seed_if_empty(self) -> None:
        """
        Insert sample requisitions when the simulated table has no rows.
        """
        with SessionLocal() as db:
            count = db.execute(
                select(sa_func.count()).select_from(ERPSimulatedRequisition)
            ).scalar() or 0

            if count > 0:
                logger.info(
                    "MockERPAdapter: simulated table already has %d rows — skipping seed",
                    count,
                )
                return

            for data in self._SEED_DATA:
                row = ERPSimulatedRequisition(
                    erp_requisition_id=data["erp_requisition_id"],
                    item_number=data.get("item_number"),
                    material=data.get("material"),
                    description=data.get("description"),
                    quantity=data.get("quantity"),
                    unit=data.get("unit"),
                    price=data.get("price"),
                    currency=data.get("currency"),
                    plant=data.get("plant"),
                    status="pending",
                )
                db.add(row)

            db.commit()
            logger.info(
                "MockERPAdapter: seeded %d sample requisitions",
                len(self._SEED_DATA),
            )

    @staticmethod
    def _get_or_create(
        requisition_id: str,
        db: Session,
    ) -> ERPSimulatedRequisition:
        """
        Return the row for *requisition_id*, creating a pending stub if it
        doesn't exist yet (mirrors a real ERP where the record is always
        present).
        """
        stmt = select(ERPSimulatedRequisition).where(
            ERPSimulatedRequisition.erp_requisition_id == requisition_id
        )
        row = db.execute(stmt).scalar_one_or_none()

        if row is None:
            logger.info(
                "MockERPAdapter: auto-creating pending requisition %s",
                requisition_id,
            )
            row = ERPSimulatedRequisition(
                erp_requisition_id=requisition_id,
                description=f"[Auto-created] {requisition_id}",
                status="pending",
            )
            db.add(row)
            db.flush()  # Ensure row.id is populated

        return row

    @staticmethod
    def _row_to_dto(row: ERPSimulatedRequisition) -> RequisitionDTO:
        """Convert an ORM row to a framework-agnostic DTO."""
        return RequisitionDTO(
            erp_requisition_id=row.erp_requisition_id,
            item_number=row.item_number,
            material=row.material,
            description=row.description,
            quantity=row.quantity,
            unit=row.unit,
            price=row.price,
            currency=row.currency,
            plant=row.plant,
            fetched_at=row.last_updated_at or row.created_at,
        )
