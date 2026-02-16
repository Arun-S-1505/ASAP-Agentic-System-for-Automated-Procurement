"""
SAPERPAdapter — Enterprise-grade SAP S/4HANA OData connector.

Implements the ERPAdapter interface for real SAP systems using the
Purchase Requisition Processing OData API (API_PURCHASEREQ_PROCESS_SRV).

Features:
    - Basic authentication with SAP credentials
    - CSRF token handling for write operations (POST/PATCH)
    - Configurable timeout per request
    - Automatic retry with exponential backoff
    - Comprehensive error handling and logging
    - OData response parsing with field normalization
    - Fully interchangeable with MockERPAdapter

OData Endpoints:
    - GET  .../A_PurchaseRequisitionItem         → fetch pending requisitions
    - GET  .../A_PurchaseRequisitionItem(...)     → single requisition details
    - POST .../PurchaseRequisitionRelease         → approve / reject actions
"""

import logging
import time
from datetime import datetime
from typing import Any, Optional

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from .base import ERPAdapter
from config import get_settings
from models.domain import RequisitionDTO

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
_SERVICE_PATH = "/sap/opu/odata/sap/API_PURCHASEREQ_PROCESS_SRV"
_ENTITY_SET = "A_PurchaseRequisitionItem"

# OData field names → RequisitionDTO field names
_ODATA_FIELD_MAP: dict[str, str] = {
    "PurchaseRequisition":          "erp_requisition_id",
    "PurchaseRequisitionItem":      "item_number",
    "Material":                     "material",
    "PurchaseRequisitionItemText":  "description",
    "RequestedQuantity":            "quantity",
    "BaseUnit":                     "unit",
    "PurchaseRequisitionPrice":     "price",
    "PurReqnItemCurrency":          "currency",
    "Plant":                        "plant",
}

# Decision mapping → SAP release action codes
_SAP_ACTION_MAP: dict[str, str] = {
    "approve":          "01",    # Release
    "auto_approve":     "01",    # Release
    "manual_approve":   "01",    # Release
    "reject":           "02",    # Reject
    "hold":             "03",    # Reset / Hold
    "cancel":           "03",    # Reset / Hold
}

# Retry configuration
_MAX_RETRIES = 3
_BACKOFF_FACTOR = 0.5
_RETRY_STATUS_CODES = frozenset({429, 500, 502, 503, 504})


class SAPERPAdapter(ERPAdapter):
    """
    Production-ready adapter for SAP S/4HANA via OData REST APIs.

    This adapter communicates with the SAP Purchase Requisition Processing
    service to fetch, approve, and reject purchase requisitions.

    Configuration (via environment / .env):
        SAP_BASE_URL    — e.g. https://myerp.example.com
        SAP_USERNAME    — SAP user ID
        SAP_PASSWORD    — SAP password
        SAP_TIMEOUT     — request timeout in seconds (default 30)

    Usage:
        adapter = SAPERPAdapter()
        adapter.connect()
        reqs = adapter.fetch_pending_requisitions()
        adapter.submit_approval("PR-001", "approve", "Looks good")
        adapter.disconnect()
    """

    def __init__(self) -> None:
        self._settings = get_settings()
        self._session: Optional[requests.Session] = None
        self._csrf_token: Optional[str] = None
        self._connected: bool = False

    # ------------------------------------------------------------------
    # Lifecycle
    # ------------------------------------------------------------------

    def connect(self) -> None:
        """
        Create an authenticated HTTP session to SAP with retry strategy.

        Sets up:
            - API Key authentication (for SAP API Business Hub Sandbox)
            - OR Basic authentication (for on-premise / BTP Trial)
            - Default headers (Accept: application/json)
            - Automatic retry on transient failures
            - SSL verification enabled

        Validates connectivity by fetching a CSRF token from SAP.
        """
        if self._connected and self._session:
            logger.debug("SAPERPAdapter: already connected — skipping")
            return

        logger.info(
            "SAPERPAdapter: connecting to %s (user=%s)",
            self._settings.sap_base_url,
            self._settings.sap_username or "(API key)",
        )

        # Build session with retry strategy
        self._session = requests.Session()

        retry_strategy = Retry(
            total=_MAX_RETRIES,
            backoff_factor=_BACKOFF_FACTOR,
            status_forcelist=list(_RETRY_STATUS_CODES),
            allowed_methods=["GET", "POST", "PATCH"],
            raise_on_status=False,
        )
        adapter = HTTPAdapter(max_retries=retry_strategy)
        self._session.mount("https://", adapter)
        self._session.mount("http://", adapter)

        # Authentication strategy
        if self._settings.sap_api_key:
            # SAP API Business Hub Sandbox — API Key auth
            logger.info("SAPERPAdapter: using API Key authentication (Sandbox)")
            self._session.headers.update({
                "APIKey": self._settings.sap_api_key,
            })
        elif self._settings.sap_username and self._settings.sap_password:
            # On-premise / BTP Trial — Basic Auth
            logger.info("SAPERPAdapter: using Basic authentication")
            self._session.auth = (
                self._settings.sap_username,
                self._settings.sap_password,
            )
        else:
            logger.warning(
                "SAPERPAdapter: no credentials configured — "
                "set SAP_API_KEY or SAP_USERNAME+SAP_PASSWORD"
            )

        # Default headers
        self._session.headers.update({
            "Accept": "application/json",
            "Content-Type": "application/json",
        })

        # Validate connection + fetch initial CSRF token
        self._fetch_csrf_token()

        self._connected = True
        logger.info("SAPERPAdapter: connection established successfully")

    def disconnect(self) -> None:
        """Close HTTP session and release resources."""
        if self._session:
            self._session.close()
            self._session = None

        self._csrf_token = None
        self._connected = False
        logger.info("SAPERPAdapter: disconnected")

    # ------------------------------------------------------------------
    # Purchase Requisition Operations
    # ------------------------------------------------------------------

    def fetch_pending_requisitions(
        self, **filters: Any,
    ) -> list[RequisitionDTO]:
        """
        Fetch pending purchase requisitions from SAP OData API.

        Queries A_PurchaseRequisitionItem with status filter for
        items awaiting release/approval.

        Args:
            filters: Optional OData filter overrides.

        Returns:
            List of normalized RequisitionDTO instances.

        Raises:
            ConnectionError: If adapter is not connected.
            RuntimeError:    If SAP response indicates failure.
        """
        self._ensure_connected()

        url = self._build_url(_ENTITY_SET)

        # OData query parameters
        params: dict[str, str] = {
            "$format": "json",
            "$top": "100",
            "$select": ",".join(_ODATA_FIELD_MAP.keys()),
        }

        # Filter for pending / not-yet-released items
        odata_filter = filters.get(
            "filter",
            "PurchaseRequisitionReleaseCode eq ''",
        )
        params["$filter"] = odata_filter

        logger.info(
            "SAPERPAdapter: fetching requisitions from %s", url,
        )
        logger.debug("SAPERPAdapter: OData params = %s", params)

        response = self._execute_request("GET", url, params=params)

        # Parse OData response envelope
        odata_results = self._extract_odata_results(response)

        # Convert to DTOs
        dtos = [
            self._map_odata_to_dto(item)
            for item in odata_results
        ]

        logger.info(
            "SAPERPAdapter: fetched %d pending requisitions", len(dtos),
        )
        return dtos

    def get_requisition_details(
        self, requisition_id: str,
    ) -> RequisitionDTO:
        """
        Fetch full details for a single requisition from SAP.

        Args:
            requisition_id: SAP purchase requisition number.

        Returns:
            Normalized RequisitionDTO instance.

        Raises:
            ConnectionError: If adapter is not connected.
            RuntimeError:    If requisition not found or SAP error.
        """
        self._ensure_connected()

        url = self._build_url(_ENTITY_SET)

        params: dict[str, str] = {
            "$format": "json",
            "$filter": f"PurchaseRequisition eq '{requisition_id}'",
            "$top": "1",
        }

        logger.info(
            "SAPERPAdapter: fetching details for %s", requisition_id,
        )

        response = self._execute_request("GET", url, params=params)
        results = self._extract_odata_results(response)

        if not results:
            logger.warning(
                "SAPERPAdapter: requisition %s not found in SAP",
                requisition_id,
            )
            return RequisitionDTO(
                erp_requisition_id=requisition_id,
                description=f"[SAP] Not found: {requisition_id}",
                fetched_at=datetime.utcnow(),
            )

        return self._map_odata_to_dto(results[0])

    # ------------------------------------------------------------------
    # Approval Operations
    # ------------------------------------------------------------------

    def submit_approval(
        self,
        requisition_id: str,
        decision: str,
        comment: str = "",
    ) -> dict[str, Any]:
        """
        Submit an approval or rejection decision to SAP.

        Calls the PurchaseRequisitionRelease function import (or action)
        on the OData service to release or reject a requisition.

        Args:
            requisition_id: SAP purchase requisition number.
            decision: One of: approve, auto_approve, manual_approve,
                      reject, hold, cancel.
            comment: Optional reason text.

        Returns:
            Confirmation payload with status and SAP response details.

        Raises:
            ConnectionError: If adapter is not connected.
            RuntimeError:    If SAP rejects the operation.
        """
        self._ensure_connected()

        # Map middleware decision to SAP action code
        action_code = _SAP_ACTION_MAP.get(decision, "03")

        # Build the release action payload
        payload = {
            "PurchaseRequisition": requisition_id,
            "PurchaseReqnReleaseCode": action_code,
        }

        if comment:
            payload["Note"] = comment[:256]  # SAP field length limit

        # Ensure fresh CSRF token for write operation
        self._fetch_csrf_token()

        url = self._build_url("PurchaseRequisitionRelease")

        logger.info(
            "SAPERPAdapter: submitting %s for %s (action_code=%s)",
            decision,
            requisition_id,
            action_code,
        )

        try:
            response = self._execute_request(
                "POST",
                url,
                json_data=payload,
            )

            logger.info(
                "SAPERPAdapter: successfully submitted %s for %s",
                decision,
                requisition_id,
            )

            return {
                "status": "ok",
                "requisition_id": requisition_id,
                "decision": decision,
                "action_code": action_code,
                "sap_response": response,
            }

        except RuntimeError as exc:
            logger.error(
                "SAPERPAdapter: submit_approval failed for %s — %s",
                requisition_id,
                exc,
            )
            return {
                "status": "error",
                "requisition_id": requisition_id,
                "decision": decision,
                "error": str(exc),
            }

    # ------------------------------------------------------------------
    # Health
    # ------------------------------------------------------------------

    def health_check(self) -> dict[str, Any]:
        """
        Verify SAP connectivity by pinging the OData $metadata endpoint.

        Returns:
            Health status dict with adapter info and response code.
        """
        try:
            prefix = self._settings.sap_service_prefix
            url = f"{self._settings.sap_base_url}{prefix}{_SERVICE_PATH}/$metadata"

            resp = None
            if self._session:
                resp = self._session.get(
                    url, timeout=self._settings.sap_timeout,
                )

            if resp and resp.ok:
                return {
                    "status": "healthy",
                    "adapter": "sap",
                    "base_url": self._settings.sap_base_url,
                    "status_code": resp.status_code,
                    "connected": self._connected,
                }

            return {
                "status": "unhealthy",
                "adapter": "sap",
                "base_url": self._settings.sap_base_url,
                "status_code": getattr(resp, "status_code", None),
                "connected": self._connected,
            }

        except Exception as exc:
            logger.error("SAPERPAdapter: health check failed — %s", exc)
            return {
                "status": "unhealthy",
                "adapter": "sap",
                "error": str(exc),
                "connected": self._connected,
            }

    # ------------------------------------------------------------------
    # Private: HTTP request execution
    # ------------------------------------------------------------------

    def _execute_request(
        self,
        method: str,
        url: str,
        params: Optional[dict[str, str]] = None,
        json_data: Optional[dict[str, Any]] = None,
    ) -> dict[str, Any]:
        """
        Execute an HTTP request against SAP with error handling.

        For write operations (POST/PATCH), the CSRF token is injected
        into the request headers automatically.

        Args:
            method:    HTTP method (GET, POST, PATCH).
            url:       Full endpoint URL.
            params:    Query string parameters.
            json_data: JSON body for POST/PATCH.

        Returns:
            Parsed JSON response body.

        Raises:
            ConnectionError: If session is not initialized.
            RuntimeError:    If request fails or SAP returns an error.
        """
        if not self._session:
            raise ConnectionError(
                "SAPERPAdapter: not connected — call connect() first"
            )

        headers: dict[str, str] = {}

        # Inject CSRF token for write operations
        if method.upper() in ("POST", "PATCH", "PUT", "DELETE"):
            if self._csrf_token:
                headers["X-CSRF-Token"] = self._csrf_token

        start_time = time.monotonic()

        try:
            response = self._session.request(
                method=method,
                url=url,
                params=params,
                json=json_data,
                headers=headers,
                timeout=self._settings.sap_timeout,
            )

            elapsed_ms = (time.monotonic() - start_time) * 1000

            logger.debug(
                "SAPERPAdapter: %s %s → %d (%.0fms)",
                method,
                url,
                response.status_code,
                elapsed_ms,
            )

            # Handle CSRF token expiry (SAP returns 403)
            if response.status_code == 403 and method.upper() != "GET":
                logger.warning(
                    "SAPERPAdapter: CSRF token expired — refreshing and retrying",
                )
                self._fetch_csrf_token()
                if self._csrf_token:
                    headers["X-CSRF-Token"] = self._csrf_token

                response = self._session.request(
                    method=method,
                    url=url,
                    params=params,
                    json=json_data,
                    headers=headers,
                    timeout=self._settings.sap_timeout,
                )

            # Raise for HTTP errors
            if not response.ok:
                error_detail = self._parse_error(response)
                raise RuntimeError(
                    f"SAP OData error: HTTP {response.status_code} — "
                    f"{error_detail}"
                )

            # Parse response
            if response.content:
                return response.json()
            return {"status": "ok", "http_status": response.status_code}

        except requests.exceptions.Timeout:
            elapsed_ms = (time.monotonic() - start_time) * 1000
            logger.error(
                "SAPERPAdapter: request timed out after %.0fms — %s %s",
                elapsed_ms,
                method,
                url,
            )
            raise RuntimeError(
                f"SAP request timed out after {self._settings.sap_timeout}s"
            )

        except requests.exceptions.ConnectionError as exc:
            logger.error(
                "SAPERPAdapter: connection error — %s %s — %s",
                method,
                url,
                exc,
            )
            raise RuntimeError(f"SAP connection error: {exc}")

        except requests.exceptions.RequestException as exc:
            logger.error(
                "SAPERPAdapter: request failed — %s %s — %s",
                method,
                url,
                exc,
            )
            raise RuntimeError(f"SAP request failed: {exc}")

    # ------------------------------------------------------------------
    # Private: CSRF Token Management
    # ------------------------------------------------------------------

    def _fetch_csrf_token(self) -> None:
        """
        Fetch a CSRF token from SAP by making a GET request
        with the X-CSRF-Token: Fetch header.

        SAP requires a valid CSRF token for all write operations
        (POST, PATCH, PUT, DELETE). The token is session-scoped and
        must be refreshed periodically.
        """
        if not self._session:
            raise ConnectionError(
                "SAPERPAdapter: session not initialized — cannot fetch CSRF token"
            )

        prefix = self._settings.sap_service_prefix
        url = f"{self._settings.sap_base_url}{prefix}{_SERVICE_PATH}/"

        try:
            response = self._session.get(
                url,
                headers={"X-CSRF-Token": "Fetch"},
                timeout=self._settings.sap_timeout,
            )

            self._csrf_token = response.headers.get("X-CSRF-Token")

            if self._csrf_token:
                logger.debug("SAPERPAdapter: CSRF token acquired")
            else:
                logger.warning(
                    "SAPERPAdapter: CSRF token not returned by SAP "
                    "(status=%d)",
                    response.status_code,
                )

        except Exception as exc:
            logger.error(
                "SAPERPAdapter: failed to fetch CSRF token — %s", exc,
            )
            self._csrf_token = None

    # ------------------------------------------------------------------
    # Private: URL Building
    # ------------------------------------------------------------------

    def _build_url(self, entity_or_action: str) -> str:
        """Construct full OData URL for an entity set or function import."""
        prefix = self._settings.sap_service_prefix
        return (
            f"{self._settings.sap_base_url}"
            f"{prefix}{_SERVICE_PATH}/{entity_or_action}"
        )

    # ------------------------------------------------------------------
    # Private: Connection Guard
    # ------------------------------------------------------------------

    def _ensure_connected(self) -> None:
        """Raise if the adapter has not been connected."""
        if not self._connected or not self._session:
            raise ConnectionError(
                "SAPERPAdapter: not connected — call connect() first"
            )

    # ------------------------------------------------------------------
    # Private: OData Response Parsing
    # ------------------------------------------------------------------

    @staticmethod
    def _extract_odata_results(
        response_data: dict[str, Any],
    ) -> list[dict[str, Any]]:
        """
        Extract the results array from an OData v2/v4 response envelope.

        Handles both:
            - OData v2: { "d": { "results": [...] } }
            - OData v4: { "value": [...] }

        Args:
            response_data: Parsed JSON response body.

        Returns:
            List of result dictionaries.
        """
        # OData v2 format
        if "d" in response_data:
            d = response_data["d"]
            if isinstance(d, dict) and "results" in d:
                return d["results"]
            if isinstance(d, list):
                return d
            return [d] if isinstance(d, dict) else []

        # OData v4 format
        if "value" in response_data:
            return response_data["value"]

        # Single entity response
        if isinstance(response_data, dict) and not response_data.get("error"):
            return [response_data]

        return []

    @staticmethod
    def _map_odata_to_dto(
        odata_item: dict[str, Any],
    ) -> RequisitionDTO:
        """
        Convert raw SAP OData response item to RequisitionDTO.

        Maps SAP field names to normalized DTO fields using the
        _ODATA_FIELD_MAP constant. Handles type conversions for
        numeric fields (quantity, price).

        Args:
            odata_item: Single item from OData results array.

        Returns:
            Normalized RequisitionDTO instance.
        """
        # Parse numeric fields safely
        quantity_raw = odata_item.get("RequestedQuantity")
        price_raw = odata_item.get("PurchaseRequisitionPrice")

        quantity = None
        if quantity_raw is not None:
            try:
                quantity = float(quantity_raw)
            except (ValueError, TypeError):
                logger.warning(
                    "SAPERPAdapter: invalid quantity value: %s",
                    quantity_raw,
                )

        price = None
        if price_raw is not None:
            try:
                price = float(price_raw)
            except (ValueError, TypeError):
                logger.warning(
                    "SAPERPAdapter: invalid price value: %s",
                    price_raw,
                )

        return RequisitionDTO(
            erp_requisition_id=str(
                odata_item.get("PurchaseRequisition", "")
            ).strip(),
            item_number=odata_item.get("PurchaseRequisitionItem"),
            material=odata_item.get("Material"),
            description=odata_item.get("PurchaseRequisitionItemText"),
            quantity=quantity,
            unit=odata_item.get("BaseUnit"),
            price=price,
            currency=odata_item.get("Currency"),
            plant=odata_item.get("Plant"),
            fetched_at=datetime.utcnow(),
        )

    @staticmethod
    def _parse_error(response: requests.Response) -> str:
        """
        Extract a human-readable error message from a SAP error response.

        SAP OData errors typically follow this structure:
            { "error": { "message": { "value": "..." } } }

        Falls back to the raw response text if parsing fails.

        Args:
            response: Failed HTTP response object.

        Returns:
            Error message string.
        """
        try:
            error_body = response.json()
            error_obj = error_body.get("error", {})
            message_obj = error_obj.get("message", {})

            if isinstance(message_obj, dict):
                return message_obj.get("value", response.text[:500])
            return str(message_obj)[:500]

        except Exception:
            return response.text[:500] if response.text else "Unknown error"
