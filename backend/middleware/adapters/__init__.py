"""
Adapters layer — ERP integration abstraction.

Provides a common interface (ERPAdapter) so the core engine
never depends on a specific ERP system.
"""

from .base import ERPAdapter
from .mock_adapter import MockERPAdapter
from .sap_adapter import SAPERPAdapter
from .hybrid_adapter import HybridERPAdapter

__all__ = ["ERPAdapter", "MockERPAdapter", "SAPERPAdapter", "HybridERPAdapter"]
