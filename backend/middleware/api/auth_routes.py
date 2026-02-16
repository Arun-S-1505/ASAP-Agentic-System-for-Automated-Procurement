"""
Authentication routes — login, register, and profile.

Mounted at /api/v1/auth in main.py.
"""

import logging

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from core.auth import (
    create_access_token,
    get_current_user,
    hash_password,
    verify_password,
)
from db.session import get_db
from db.user_model import User

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/auth", tags=["Authentication"])


# ---------------------------------------------------------------------------
# Request / Response schemas
# ---------------------------------------------------------------------------


class LoginRequest(BaseModel):
    username: str
    password: str


class RegisterRequest(BaseModel):
    username: str
    password: str
    full_name: str = ""


class UserResponse(BaseModel):
    username: str
    full_name: str
    role: str


class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------


@router.post("/login", response_model=AuthResponse)
def login(body: LoginRequest, db: Session = Depends(get_db)):
    """
    Authenticate user and return JWT access token.

    Raises:
        HTTPException 401 if credentials are invalid.
    """
    stmt = select(User).where(User.username == body.username)
    user = db.execute(stmt).scalar_one_or_none()

    if not user or not verify_password(body.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )

    token = create_access_token(
        data={"sub": user.username, "role": user.role},
    )

    logger.info("User '%s' logged in successfully", user.username)

    return AuthResponse(
        access_token=token,
        user=UserResponse(
            username=user.username,
            full_name=user.full_name,
            role=user.role,
        ),
    )


@router.post(
    "/register",
    response_model=AuthResponse,
    status_code=status.HTTP_201_CREATED,
)
def register(body: RegisterRequest, db: Session = Depends(get_db)):
    """
    Create a new user account and return JWT access token.

    Raises:
        HTTPException 409 if username already exists.
    """
    # Check for existing user
    existing = db.execute(
        select(User).where(User.username == body.username),
    ).scalar_one_or_none()

    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Username '{body.username}' already exists",
        )

    # Create user
    user = User(
        username=body.username,
        hashed_password=hash_password(body.password),
        full_name=body.full_name or body.username,
        role="approver",
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    # Generate token
    token = create_access_token(
        data={"sub": user.username, "role": user.role},
    )

    logger.info("User '%s' registered successfully", user.username)

    return AuthResponse(
        access_token=token,
        user=UserResponse(
            username=user.username,
            full_name=user.full_name,
            role=user.role,
        ),
    )


@router.get("/me", response_model=UserResponse)
def get_profile(
    current_user: dict = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Return current user's profile from token."""
    user = db.execute(
        select(User).where(User.username == current_user["username"]),
    ).scalar_one_or_none()

    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    return UserResponse(
        username=user.username,
        full_name=user.full_name,
        role=user.role,
    )
