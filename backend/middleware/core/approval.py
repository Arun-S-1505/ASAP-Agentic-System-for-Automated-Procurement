"""
ApprovalEngine — Decides approval outcome based on risk score and policy rules.

This module is a PURE domain component:
    - No database access
    - No HTTP calls
    - No framework imports
"""

import logging
from typing import Any

logger = logging.getLogger(__name__)


class ApprovalEngine:
    """
    Takes a risk score + requisition context and returns an approval decision.

    Decisions:
        "auto_approve"  — low risk, within policy, no human needed
        "escalate"      — medium risk, route to approver
        "reject"        — policy violation or high risk
        "hold"          — needs additional information
    """

    # TODO: make thresholds configurable via Settings or DB
    LOW_RISK_THRESHOLD: float = 30.0
    HIGH_RISK_THRESHOLD: float = 70.0

    def decide(self, risk_score: float, requisition: dict[str, Any]) -> str:
        """
        Produce an approval decision.

        Args:
            risk_score: Output of RiskScoringEngine.score() — range 0–100.
            requisition: Normalised requisition dict.

        Returns:
            One of: "auto_approve", "escalate", "reject", "hold".
        """
        material: str = (requisition.get("material") or "").upper()
        plant: str = (requisition.get("plant") or "").upper()

        # Policy override: restricted materials always escalate regardless of score
        HIGH_RISK_KEYWORDS = {
            "HAZMAT", "CHEM", "CHEMICAL", "EXPLOSIVE",
            "RADIOACTIVE", "BIOHAZARD", "TOXIC",
        }
        RESTRICTED_PLANTS = {"NUCLEAR", "DEFENSE", "WEAPONS", "CLASSIFIED"}

        if any(kw in material for kw in HIGH_RISK_KEYWORDS):
            logger.debug(
                "ApprovalEngine: high-risk material detected — escalating"
            )
            return "escalate"

        if any(rp in plant for rp in RESTRICTED_PLANTS):
            logger.debug(
                "ApprovalEngine: restricted plant detected — escalating"
            )
            return "escalate"

        # Score-based decision matrix
        if risk_score <= self.LOW_RISK_THRESHOLD:
            logger.debug(
                "ApprovalEngine: score %.2f ≤ %.2f → auto_approve",
                risk_score, self.LOW_RISK_THRESHOLD,
            )
            return "auto_approve"

        if risk_score <= self.HIGH_RISK_THRESHOLD:
            logger.debug(
                "ApprovalEngine: score %.2f in (%.2f, %.2f] → escalate",
                risk_score, self.LOW_RISK_THRESHOLD, self.HIGH_RISK_THRESHOLD,
            )
            return "escalate"

        logger.debug(
            "ApprovalEngine: score %.2f > %.2f → reject",
            risk_score, self.HIGH_RISK_THRESHOLD,
        )
        return "reject"
