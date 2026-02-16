"""
ApprovalOrchestrator Usage Examples
====================================

This file shows how to use the ApprovalOrchestrator for the grace-buffer
approval system.
"""

from db.session import SessionLocal
from adapters import MockERPAdapter
from services.orchestrator import ApprovalOrchestrator

# ---------------------------------------------------------------------------
# Example 1: Detect and Stage Requisitions
# ---------------------------------------------------------------------------

def example_detect_and_stage():
    """Fetch from ERP, score, decide, and stage for commit."""
    
    # Initialize adapter and connect
    adapter = MockERPAdapter()
    adapter.connect()
    
    # Initialize orchestrator
    orchestrator = ApprovalOrchestrator()
    
    # Use database session
    with SessionLocal() as db:
        # Detect and stage requisitions
        staged_decisions = orchestrator.detect_and_stage_requisitions(adapter, db)
        
        print(f"Staged {len(staged_decisions)} decisions")
        
        for decision in staged_decisions:
            print(f"\n  Requisition: {decision.erp_requisition_id}")
            print(f"  Risk Score:  {decision.risk_score:.2f}")
            print(f"  Decision:    {decision.decision}")
            print(f"  State:       {decision.state}")
            print(f"  Commit At:   {decision.commit_at}")
    
    adapter.disconnect()


# ---------------------------------------------------------------------------
# Example 2: Undo (Cancel) a Decision
# ---------------------------------------------------------------------------

def example_undo_decision():
    """Cancel a pending commit before grace period expires."""
    
    orchestrator = ApprovalOrchestrator()
    
    with SessionLocal() as db:
        # Cancel a specific requisition
        cancelled = orchestrator.undo_decision("PR-2026-001", db)
        
        if cancelled:
            print(f"✓ Cancelled decision for {cancelled.erp_requisition_id}")
            print(f"  New state: {cancelled.state}")
        else:
            print("✗ No pending decision found to cancel")


# ---------------------------------------------------------------------------
# Example 3: Query Commit-Ready Decisions
# ---------------------------------------------------------------------------

def example_list_pending_commits():
    """Find decisions ready for ERP commit (grace period expired)."""
    
    orchestrator = ApprovalOrchestrator()
    
    with SessionLocal() as db:
        # Get decisions where commit_at <= now
        ready_for_commit = orchestrator.list_pending_commits(db)
        
        print(f"Found {len(ready_for_commit)} decisions ready for commit")
        
        for decision in ready_for_commit:
            print(f"\n  ID:          {decision.id}")
            print(f"  Requisition: {decision.erp_requisition_id}")
            print(f"  Decision:    {decision.decision}")
            print(f"  Risk Score:  {decision.risk_score:.2f}")
            print(f"  Commit At:   {decision.commit_at}")


# ---------------------------------------------------------------------------
# Example 4: Full Workflow (API Handler Pattern)
# ---------------------------------------------------------------------------

def example_api_handler():
    """Typical pattern for API endpoint handler."""
    
    from fastapi import Depends
    from sqlalchemy.orm import Session
    from db.session import get_db
    
    # Simulated API endpoint logic
    def sync_endpoint(db: Session = Depends(get_db)):
        """POST /api/v1/requisitions/sync"""
        
        # Connect to ERP
        adapter = MockERPAdapter()
        adapter.connect()
        
        try:
            # Detect and stage
            orchestrator = ApprovalOrchestrator()
            staged = orchestrator.detect_and_stage_requisitions(adapter, db)
            
            return {
                "synced": len(staged),
                "message": f"Staged {len(staged)} decisions for commit",
                "grace_period_minutes": orchestrator.settings.grace_period_minutes,
            }
        finally:
            adapter.disconnect()


# ---------------------------------------------------------------------------
# Example 5: Scheduler Pattern (Background Worker)
# ---------------------------------------------------------------------------

def example_scheduler_job():
    """Pattern for background commit worker."""
    
    orchestrator = ApprovalOrchestrator()
    
    with SessionLocal() as db:
        # Get decisions ready for commit
        ready = orchestrator.list_pending_commits(db)
        
        if not ready:
            print("No decisions ready for commit")
            return
        
        print(f"Processing {len(ready)} decisions...")
        
        # TODO: For each decision, call adapter.submit_approval()
        # TODO: Update state to "committed" or "failed"
        # TODO: Set committed_at timestamp
        
        # This commit logic will be implemented in a separate module


if __name__ == "__main__":
    print("=" * 60)
    print("ApprovalOrchestrator Usage Examples")
    print("=" * 60)
    print("\nNote: These examples require a configured MySQL database.")
    print("See .env.example for database configuration.\n")
