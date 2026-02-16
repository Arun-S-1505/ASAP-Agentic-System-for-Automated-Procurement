"""
RiskExplanationEngine — Generates human-readable risk explanations.

This module is a PURE domain component:
    - No database access
    - No HTTP calls
    - No framework imports
    - Stateless — receives data, returns explanations

Implements rule-based "Explainable AI" for enterprise procurement decisions.
Each rule evaluates a specific aspect of the requisition and produces a
natural-language explanation when triggered.
"""

import logging
from typing import Optional

from models.domain import RequisitionDTO

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Configuration constants — thresholds and categories
# ---------------------------------------------------------------------------

# Value thresholds (aligned with RiskScoringEngine — tuned for SAP Sandbox)
HIGH_VALUE_THRESHOLD: float = 100.0         # prod: 50_000.0
ELEVATED_VALUE_THRESHOLD: float = 50.0      # prod: 10_000.0
MODERATE_VALUE_THRESHOLD: float = 20.0      # prod: 5_000.0

# Unit-price threshold
UNIT_PRICE_THRESHOLD: float = 10.0          # prod: 5_000.0

# Quantity threshold
QUANTITY_THRESHOLD: float = 5.0             # prod: 500.0

# High-risk material keywords (case-insensitive match)
HIGH_RISK_MATERIALS: set[str] = {
    "HAZMAT", "CHEM", "CHEMICAL", "EXPLOSIVE",
    "RADIOACTIVE", "BIOHAZARD", "TOXIC",
}

# Restricted / sensitive plant codes
RESTRICTED_PLANTS: set[str] = {
    "NUCLEAR", "DEFENSE", "WEAPONS", "CLASSIFIED",
}


class RiskExplanationEngine:
    """
    Generates human-readable, rule-based risk explanations for procurement
    requisitions.

    Each public method evaluates one risk dimension and returns an optional
    explanation string.  The top-level ``explain()`` method aggregates all
    triggered rules into a single multi-line explanation.

    Design principles:
        - Pure function-style: no side-effects, no I/O
        - Each rule is independently testable
        - New rules are added by writing a ``_check_*`` method and
          registering it in ``_all_checks``

    Usage:
        engine = RiskExplanationEngine()
        explanation = engine.explain(requisition, risk_score)
    """

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def explain(
        self,
        requisition: RequisitionDTO,
        risk_score: float,
    ) -> str:
        """
        Produce a consolidated, human-readable risk explanation.

        Runs every registered check against the requisition and collects
        the triggered explanations.  If no rules fire, a default "all
        clear" message is returned.

        Args:
            requisition: Normalised requisition DTO.
            risk_score:  Pre-computed risk score (0–100).

        Returns:
            Multi-line explanation string (individual reasons separated
            by ``"; "``).
        """
        explanations: list[str] = []

        for check in self._all_checks():
            result = check(requisition, risk_score)
            if result:
                explanations.append(result)

        if not explanations:
            explanations.append(
                "All parameters within normal operational thresholds"
            )

        explanation_text = "; ".join(explanations)

        logger.debug(
            "RiskExplanationEngine: %s → %s",
            requisition.erp_requisition_id,
            explanation_text,
        )

        return explanation_text

    # ------------------------------------------------------------------
    # Rule registry
    # ------------------------------------------------------------------

    def _all_checks(self):
        """Return an ordered list of all rule-check callables."""
        return [
            self._check_high_total_value,
            self._check_elevated_total_value,
            self._check_high_unit_price,
            self._check_high_quantity,
            self._check_high_risk_material,
            self._check_restricted_plant,
            self._check_missing_price,
            self._check_missing_quantity,
        ]

    # ------------------------------------------------------------------
    # Individual rule checks
    # ------------------------------------------------------------------

    @staticmethod
    def _check_high_total_value(
        req: RequisitionDTO, _score: float,
    ) -> Optional[str]:
        """Flag when total value exceeds the high-value threshold."""
        if req.price is not None and req.quantity is not None:
            total = req.price * req.quantity
            if total > HIGH_VALUE_THRESHOLD:
                return (
                    f"Total value ₹{total:,.2f} exceeds high-value "
                    f"threshold (₹{HIGH_VALUE_THRESHOLD:,.0f})"
                )
        return None

    @staticmethod
    def _check_elevated_total_value(
        req: RequisitionDTO, _score: float,
    ) -> Optional[str]:
        """Flag when total is above elevated but below high threshold."""
        if req.price is not None and req.quantity is not None:
            total = req.price * req.quantity
            if ELEVATED_VALUE_THRESHOLD < total <= HIGH_VALUE_THRESHOLD:
                return (
                    f"Total value ₹{total:,.2f} exceeds elevated "
                    f"threshold (₹{ELEVATED_VALUE_THRESHOLD:,.0f})"
                )
        return None

    @staticmethod
    def _check_high_unit_price(
        req: RequisitionDTO, _score: float,
    ) -> Optional[str]:
        """Flag when the unit price alone is unusually high."""
        if req.price is not None and req.price > UNIT_PRICE_THRESHOLD:
            return (
                f"Unit price ₹{req.price:,.2f} exceeds historical "
                f"threshold (₹{UNIT_PRICE_THRESHOLD:,.0f})"
            )
        return None

    @staticmethod
    def _check_high_quantity(
        req: RequisitionDTO, _score: float,
    ) -> Optional[str]:
        """Flag when order quantity is above the plant-average threshold."""
        if req.quantity is not None and req.quantity > QUANTITY_THRESHOLD:
            return (
                f"Quantity {req.quantity:,.0f} exceeds plant average "
                f"threshold ({QUANTITY_THRESHOLD:,.0f})"
            )
        return None

    @staticmethod
    def _check_high_risk_material(
        req: RequisitionDTO, _score: float,
    ) -> Optional[str]:
        """Flag materials that belong to a high-risk category."""
        if req.material:
            material_upper = req.material.upper()
            for keyword in HIGH_RISK_MATERIALS:
                if keyword in material_upper:
                    return (
                        f"Material '{req.material}' classified as "
                        f"high-risk category"
                    )
        return None

    @staticmethod
    def _check_restricted_plant(
        req: RequisitionDTO, _score: float,
    ) -> Optional[str]:
        """Flag requisitions originating from restricted plants."""
        if req.plant:
            plant_upper = req.plant.upper()
            for restricted in RESTRICTED_PLANTS:
                if restricted in plant_upper:
                    return (
                        f"Requisition from restricted plant '{req.plant}'"
                    )
        return None

    @staticmethod
    def _check_missing_price(
        req: RequisitionDTO, _score: float,
    ) -> Optional[str]:
        """Flag when price data is absent (limits risk assessment accuracy)."""
        if req.price is None:
            return "Unit price data missing — unable to fully assess risk"
        return None

    @staticmethod
    def _check_missing_quantity(
        req: RequisitionDTO, _score: float,
    ) -> Optional[str]:
        """Flag when quantity data is absent."""
        if req.quantity is None:
            return "Quantity data missing — unable to fully assess risk"
        return None
