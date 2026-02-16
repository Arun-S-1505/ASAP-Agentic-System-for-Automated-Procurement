"""
RiskScoringEngine — Evaluates requisition risk for automated approval decisions.

This module is a PURE domain component:
    - No database access
    - No HTTP calls
    - No framework imports
    - Stateless — receives data, returns scores
"""

import logging

from models.domain import RequisitionDTO

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Scoring thresholds (tuned for SAP Sandbox demo data)
# Production values in comments — restore for enterprise use.
# ---------------------------------------------------------------------------
HIGH_VALUE_THRESHOLD: float = 100.0         # prod: 50_000.0
ELEVATED_VALUE_THRESHOLD: float = 50.0      # prod: 10_000.0
MODERATE_VALUE_THRESHOLD: float = 20.0      # prod: 5_000.0
UNIT_PRICE_THRESHOLD: float = 10.0          # prod: 5_000.0
QUANTITY_THRESHOLD: float = 5.0             # prod: 500.0

HIGH_RISK_MATERIALS: set[str] = {
    "HAZMAT", "CHEM", "CHEMICAL", "EXPLOSIVE",
    "RADIOACTIVE", "BIOHAZARD", "TOXIC",
}

RESTRICTED_PLANTS: set[str] = {
    "NUCLEAR", "DEFENSE", "WEAPONS", "CLASSIFIED",
}


class RiskScoringEngine:
    """
    Computes a risk score for a purchase requisition.

    The score determines whether the requisition can be auto-approved,
    needs human review, or should be flagged.

    Score ranges (example — adjust per business rules):
        0–30   → low risk   → auto-approve candidate
        31–70  → medium     → route to manager
        71–100 → high risk  → flag for audit
    """

    def score(self, requisition: RequisitionDTO) -> float:
        """
        Calculate risk score for a single requisition.

        Evaluates multiple risk dimensions:
            - Total procurement value
            - Unit price anomaly
            - Order quantity anomaly
            - High-risk material classification
            - Restricted plant origin
            - Missing data penalty

        Args:
            requisition: Normalized RequisitionDTO from adapter.

        Returns:
            Risk score between 0.0 and 100.0.
        """
        risk_score = 0.0

        # --- 1. Total-value risk ---
        if requisition.price is not None and requisition.quantity is not None:
            total_value = requisition.price * requisition.quantity

            if total_value > HIGH_VALUE_THRESHOLD:
                risk_score += 40.0
            elif total_value > ELEVATED_VALUE_THRESHOLD:
                risk_score += 20.0
            elif total_value > MODERATE_VALUE_THRESHOLD:
                risk_score += 10.0

        # --- 2. Unit-price anomaly ---
        if requisition.price is not None and requisition.price > UNIT_PRICE_THRESHOLD:
            risk_score += 15.0

        # --- 3. Quantity anomaly ---
        if requisition.quantity is not None and requisition.quantity > QUANTITY_THRESHOLD:
            risk_score += 10.0

        # --- 4. High-risk material ---
        if requisition.material:
            material_upper = requisition.material.upper()
            if any(kw in material_upper for kw in HIGH_RISK_MATERIALS):
                risk_score += 20.0

        # --- 5. Restricted plant ---
        if requisition.plant:
            plant_upper = requisition.plant.upper()
            if any(rp in plant_upper for rp in RESTRICTED_PLANTS):
                risk_score += 15.0

        # --- 6. Missing-data penalty ---
        if requisition.price is None:
            risk_score += 5.0
        if requisition.quantity is None:
            risk_score += 5.0

        final_score = min(risk_score, 100.0)

        logger.debug(
            "RiskScoringEngine.score() for %s → %.2f",
            requisition.erp_requisition_id,
            final_score,
        )

        return final_score
