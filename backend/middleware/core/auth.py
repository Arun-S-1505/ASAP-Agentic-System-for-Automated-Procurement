"""
Authentication utilities — password hashing and JWT token management.

Pure utility module:
    - No database access (caller provides user data)
    - Stateless functions
"""

import logging
from datetime import datetime, timedelta
from typing import Any

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from passlib.context import CryptContext

from config import get_settings

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Password hashing
# ---------------------------------------------------------------------------

_pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(plain: str) -> str:
    """Hash a plain-text password with bcrypt."""
    return _pwd_context.hash(plain)


def verify_password(plain: str, hashed: str) -> bool:
    """Verify a plain-text password against a bcrypt hash."""
    return _pwd_context.verify(plain, hashed)


# ---------------------------------------------------------------------------
# JWT token creation / decoding
# ---------------------------------------------------------------------------

_security = HTTPBearer()


def create_access_token(
    data: dict[str, Any],
    expires_minutes: int | None = None,
) -> str:
    """
    Create a signed JWT access token.

    Args:
        data: Payload claims (must include 'sub' for username).
        expires_minutes: Override expiry (defaults to config value).

    Returns:
        Encoded JWT string.
    """
    settings = get_settings()
    to_encode = data.copy()

    expire = datetime.utcnow() + timedelta(
        minutes=expires_minutes or settings.jwt_expiry_minutes,
    )
    to_encode.update({"exp": expire})

    token = jwt.encode(
        to_encode,
        settings.jwt_secret_key,
        algorithm=settings.jwt_algorithm,
    )
    return token


def decode_access_token(token: str) -> dict[str, Any]:
    """
    Decode and validate a JWT token.

    Returns:
        Decoded payload dict.

    Raises:
        HTTPException 401 if token is invalid or expired.
    """
    settings = get_settings()
    try:
        payload = jwt.decode(
            token,
            settings.jwt_secret_key,
            algorithms=[settings.jwt_algorithm],
        )
        return payload
    except JWTError as exc:
        logger.warning("JWT decode failed: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )


# ---------------------------------------------------------------------------
# FastAPI dependency — extracts & validates Bearer token
# ---------------------------------------------------------------------------


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(_security),
) -> dict[str, Any]:
    """
    FastAPI dependency that validates the Bearer token and returns
    the decoded user claims (sub, role, exp).

    Usage::
        @router.get("/protected")
        def protected(user: dict = Depends(get_current_user)):
            ...
    """
    payload = decode_access_token(credentials.credentials)

    username: str | None = payload.get("sub")
    if not username:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token missing 'sub' claim",
        )

    return {
        "username": username,
        "role": payload.get("role", "approver"),
    }
