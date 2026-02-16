"""
User model for JWT authentication.
"""

import uuid
from datetime import datetime

from sqlalchemy import Column, String, DateTime
from db.models import Base


class User(Base):
    """Application user for authentication."""

    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    username = Column(String(100), unique=True, nullable=False, index=True)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(200), nullable=False, default="")
    role = Column(String(50), nullable=False, default="approver")  # approver | admin
    created_at = Column(DateTime, default=datetime.utcnow)
