"""
Core layer — pure business logic with zero framework dependencies.

Contains:
    - Risk scoring engine
    - Risk explanation engine (explainable AI)
    - Approval decision logic
"""

from .risk_scoring import RiskScoringEngine
from .risk_explanation import RiskExplanationEngine
from .approval import ApprovalEngine

__all__ = ["RiskScoringEngine", "RiskExplanationEngine", "ApprovalEngine"]
