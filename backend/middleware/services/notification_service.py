"""
NotificationService — Simulates enterprise notifications after ERP commit.

This service generates mock email and Slack notifications for committed
approval decisions.  No real API calls are made — the notifications are
logged to the database for audit and UI display.

Architecture:
    - Pure service layer (no route/framework logic)
    - Receives a committed ApprovalDecision + DB session
    - Inserts NotificationLog rows for each simulated channel
"""

import logging
from datetime import datetime

from sqlalchemy.orm import Session

from db.models import ApprovalDecision, NotificationLog

logger = logging.getLogger(__name__)


class NotificationService:
    """
    Simulates post-commit notifications across multiple channels.

    Supported channels:
        - email  — simulated enterprise email notification
        - slack  — simulated Slack workspace notification

    Usage:
        service = NotificationService()
        service.send_post_commit_notifications(decision, db)
    """

    def send_post_commit_notifications(
        self,
        decision: ApprovalDecision,
        db: Session,
    ) -> list[NotificationLog]:
        """
        Dispatch simulated notifications for a committed decision.

        Creates one notification log per channel (email + Slack).
        Failures in one channel do not block the other.

        Args:
            decision: The committed ApprovalDecision record.
            db: Active SQLAlchemy session (caller manages commit).

        Returns:
            List of created NotificationLog records.
        """
        logger.info(
            "NotificationService: sending notifications for %s (decision=%s)",
            decision.erp_requisition_id,
            decision.decision,
        )

        logs: list[NotificationLog] = []

        # Email notification
        email_log = self._send_email_notification(decision, db)
        logs.append(email_log)

        # Slack notification
        slack_log = self._send_slack_notification(decision, db)
        logs.append(slack_log)

        logger.info(
            "NotificationService: dispatched %d notifications for %s",
            len(logs),
            decision.erp_requisition_id,
        )

        return logs

    # ------------------------------------------------------------------
    # Channel-specific senders
    # ------------------------------------------------------------------

    def _send_email_notification(
        self,
        decision: ApprovalDecision,
        db: Session,
    ) -> NotificationLog:
        """Simulate an enterprise email notification."""
        message = self._build_email_message(decision)

        log = NotificationLog(
            erp_requisition_id=decision.erp_requisition_id,
            decision=decision.decision,
            channel="email",
            status="sent",
            message=message,
        )

        db.add(log)

        logger.debug(
            "NotificationService: [EMAIL] simulated for %s",
            decision.erp_requisition_id,
        )

        return log

    def _send_slack_notification(
        self,
        decision: ApprovalDecision,
        db: Session,
    ) -> NotificationLog:
        """Simulate a Slack workspace notification."""
        message = self._build_slack_message(decision)

        log = NotificationLog(
            erp_requisition_id=decision.erp_requisition_id,
            decision=decision.decision,
            channel="slack",
            status="sent",
            message=message,
        )

        db.add(log)

        logger.debug(
            "NotificationService: [SLACK] simulated for %s",
            decision.erp_requisition_id,
        )

        return log

    # ------------------------------------------------------------------
    # Message builders
    # ------------------------------------------------------------------

    @staticmethod
    def _build_email_message(decision: ApprovalDecision) -> str:
        """
        Build a realistic enterprise email notification body.

        Args:
            decision: The committed approval decision.

        Returns:
            Formatted email message string.
        """
        decision_label = decision.decision.replace("_", " ").title()
        risk_score = decision.risk_score or 0.0

        lines = [
            f"Subject: ERP Approval Decision — {decision.erp_requisition_id}",
            "",
            f"Requisition ID : {decision.erp_requisition_id}",
            f"Decision       : {decision_label}",
            f"Risk Score     : {risk_score:.2f}",
            f"Committed At   : {decision.committed_at or datetime.utcnow()}",
            "",
        ]

        if decision.risk_explanation:
            lines.append(f"Risk Analysis  : {decision.risk_explanation}")
            lines.append("")

        lines.extend([
            "This is an automated notification from the ERP Approval Middleware.",
            "Please review in the approval dashboard if action is required.",
            "",
            "— ASAP Procurement System",
        ])

        return "\n".join(lines)

    @staticmethod
    def _build_slack_message(decision: ApprovalDecision) -> str:
        """
        Build a realistic Slack notification message.

        Args:
            decision: The committed approval decision.

        Returns:
            Formatted Slack message string.
        """
        decision_label = decision.decision.replace("_", " ").title()
        risk_score = decision.risk_score or 0.0

        # Emoji mapping for decision type
        emoji_map = {
            "auto_approve": "✅",
            "manual_approve": "👀",
            "hold": "⏸️",
            "reject": "❌",
        }
        emoji = emoji_map.get(decision.decision, "📋")

        lines = [
            f"{emoji} *ERP Approval Decision*",
            f"• *Requisition:* `{decision.erp_requisition_id}`",
            f"• *Decision:* {decision_label}",
            f"• *Risk Score:* {risk_score:.2f}",
        ]

        if decision.risk_explanation:
            lines.append(f"• *Risk Analysis:* {decision.risk_explanation}")

        lines.append(
            f"• *Committed:* {decision.committed_at or datetime.utcnow()}"
        )

        return "\n".join(lines)
