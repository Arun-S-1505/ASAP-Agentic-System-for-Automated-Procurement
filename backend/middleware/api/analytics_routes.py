"""
Analytics routes — aggregated decision statistics.

Provides:
    GET /analytics/summary — decision counts, risk distribution, daily trend
"""

import logging
from datetime import datetime, timedelta

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import func, case, cast, Date
from sqlalchemy.orm import Session

from core.auth import get_current_user
from db.models import ApprovalDecision
from db.session import get_db

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/analytics", tags=["analytics"])


# ---------------------------------------------------------------------------
# Response schemas
# ---------------------------------------------------------------------------

class DailyCount(BaseModel):
    date: str       # ISO date string  e.g. "2026-02-15"
    count: int


class RiskDistribution(BaseModel):
    low: int        # risk_score < 30
    medium: int     # 30 <= risk_score < 70
    high: int       # risk_score >= 70


class AnalyticsSummary(BaseModel):
    total_decisions: int
    auto_approved: int
    manual_approved: int
    held: int
    rejected: int
    avg_risk_score: float
    automation_rate: float          # percentage 0–100
    risk_distribution: RiskDistribution
    daily_counts: list[DailyCount]  # last 7 days


# ---------------------------------------------------------------------------
# GET /analytics/summary
# ---------------------------------------------------------------------------

@router.get("/summary", response_model=AnalyticsSummary)
def analytics_summary(
    db: Session = Depends(get_db),
    _user: dict = Depends(get_current_user),
):
    """
    Return aggregated analytics computed from the approval_decisions table.
    """
    logger.info("GET /analytics/summary")

    # ── Decision counts ──
    total = db.query(func.count(ApprovalDecision.id)).scalar() or 0

    auto_approved = (
        db.query(func.count(ApprovalDecision.id))
        .filter(ApprovalDecision.decision == "auto_approve")
        .scalar()
    ) or 0

    manual_approved = (
        db.query(func.count(ApprovalDecision.id))
        .filter(ApprovalDecision.decision == "manual_approve")
        .scalar()
    ) or 0

    held = (
        db.query(func.count(ApprovalDecision.id))
        .filter(ApprovalDecision.decision == "hold")
        .scalar()
    ) or 0

    rejected = (
        db.query(func.count(ApprovalDecision.id))
        .filter(ApprovalDecision.decision == "reject")
        .scalar()
    ) or 0

    # ── Average risk score ──
    avg_risk = db.query(func.avg(ApprovalDecision.risk_score)).scalar() or 0.0

    # ── Automation rate ──
    automation_rate = (auto_approved / total * 100) if total > 0 else 0.0

    # ── Risk distribution ──
    low = (
        db.query(func.count(ApprovalDecision.id))
        .filter(ApprovalDecision.risk_score < 30)
        .scalar()
    ) or 0

    medium = (
        db.query(func.count(ApprovalDecision.id))
        .filter(ApprovalDecision.risk_score >= 30, ApprovalDecision.risk_score < 70)
        .scalar()
    ) or 0

    high = (
        db.query(func.count(ApprovalDecision.id))
        .filter(ApprovalDecision.risk_score >= 70)
        .scalar()
    ) or 0

    # ── Daily counts (last 7 days) ──
    today = datetime.utcnow().date()
    week_ago = today - timedelta(days=6)

    daily_rows = (
        db.query(
            cast(ApprovalDecision.created_at, Date).label("day"),
            func.count(ApprovalDecision.id).label("cnt"),
        )
        .filter(cast(ApprovalDecision.created_at, Date) >= week_ago)
        .group_by(cast(ApprovalDecision.created_at, Date))
        .order_by(cast(ApprovalDecision.created_at, Date))
        .all()
    )

    # Build a full 7-day series (fill missing days with 0)
    daily_map = {str(r.day): r.cnt for r in daily_rows}
    daily_counts = []
    for i in range(7):
        d = week_ago + timedelta(days=i)
        daily_counts.append(DailyCount(date=str(d), count=daily_map.get(str(d), 0)))

    return AnalyticsSummary(
        total_decisions=total,
        auto_approved=auto_approved,
        manual_approved=manual_approved,
        held=held,
        rejected=rejected,
        avg_risk_score=round(float(avg_risk), 2),
        automation_rate=round(automation_rate, 1),
        risk_distribution=RiskDistribution(low=low, medium=medium, high=high),
        daily_counts=daily_counts,
    )
