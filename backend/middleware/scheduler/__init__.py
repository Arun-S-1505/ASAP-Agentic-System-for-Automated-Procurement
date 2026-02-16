"""
Scheduler layer — background workers powered by APScheduler.
"""

from .worker import start_scheduler, shutdown_scheduler, init_adapter

__all__ = ["start_scheduler", "shutdown_scheduler"]
