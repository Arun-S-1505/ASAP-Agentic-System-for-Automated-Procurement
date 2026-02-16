package com.arun.asap.core.network

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.AnalyticsSummaryDto
import com.arun.asap.data.model.ApproveRejectRequest
import com.arun.asap.data.model.ApproveRejectResponse
import com.arun.asap.data.model.AuthResponse
import com.arun.asap.data.model.BatchRequest
import com.arun.asap.data.model.BatchResponse
import com.arun.asap.data.model.DecisionListResponse
import com.arun.asap.data.model.DetectResponse
import com.arun.asap.data.model.LoginRequest
import com.arun.asap.data.model.NotificationListResponse
import com.arun.asap.data.model.RegisterRequest
import com.arun.asap.data.model.UndoResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * API client for handling network requests.
 * Follows clean architecture principles with separation of concerns.
 *
 * @param httpClient The configured HttpClient instance
 * @param baseUrl The base URL for API endpoints
 */
class ApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {

    // ------------------------------------------------------------------
    // Auth helpers
    // ------------------------------------------------------------------

    /** Build a request with the Bearer token if available. */
    private fun HttpRequestBuilder.withAuth() {
        TokenManager.getToken()?.let { token ->
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    // ------------------------------------------------------------------
    // Auth endpoints (no token required)
    // ------------------------------------------------------------------

    suspend fun login(username: String, password: String): ApiResult<AuthResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username = username, password = password))
            }.body()
        }
    }

    suspend fun register(
        username: String,
        password: String,
        fullName: String = ""
    ): ApiResult<AuthResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username = username, password = password, fullName = fullName))
            }.body()
        }
    }

    // ------------------------------------------------------------------
    // Protected endpoints (token required)
    // ------------------------------------------------------------------

    suspend fun detect(): ApiResult<DetectResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/detect") {
                withAuth()
            }.body()
        }
    }

    suspend fun getDecisions(): ApiResult<DecisionListResponse> {
        return safeApiCall {
            httpClient.get("$baseUrl/decisions") {
                withAuth()
            }.body()
        }
    }

    suspend fun undo(requisitionId: String): ApiResult<UndoResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/undo/$requisitionId") {
                withAuth()
            }.body()
        }
    }

    suspend fun approve(
        requisitionId: String,
        comment: String? = null
    ): ApiResult<ApproveRejectResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/approve/$requisitionId") {
                withAuth()
                contentType(ContentType.Application.Json)
                setBody(ApproveRejectRequest(comment = comment))
            }.body()
        }
    }

    suspend fun reject(
        requisitionId: String,
        comment: String? = null
    ): ApiResult<ApproveRejectResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/reject/$requisitionId") {
                withAuth()
                contentType(ContentType.Application.Json)
                setBody(ApproveRejectRequest(comment = comment))
            }.body()
        }
    }

    suspend fun getNotifications(): ApiResult<NotificationListResponse> {
        return safeApiCall {
            httpClient.get("$baseUrl/notifications") {
                withAuth()
            }.body()
        }
    }

    // ------------------------------------------------------------------
    // Analytics endpoints
    // ------------------------------------------------------------------

    suspend fun getAnalytics(): ApiResult<AnalyticsSummaryDto> {
        return safeApiCall {
            httpClient.get("$baseUrl/analytics/summary") {
                withAuth()
            }.body()
        }
    }

    // ------------------------------------------------------------------
    // Batch endpoints
    // ------------------------------------------------------------------

    suspend fun batchApprove(
        ids: List<String>,
        comment: String? = null
    ): ApiResult<BatchResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/batch/approve") {
                withAuth()
                contentType(ContentType.Application.Json)
                setBody(BatchRequest(ids = ids, comment = comment))
            }.body()
        }
    }

    suspend fun batchReject(
        ids: List<String>,
        comment: String? = null
    ): ApiResult<BatchResponse> {
        return safeApiCall {
            httpClient.post("$baseUrl/batch/reject") {
                withAuth()
                contentType(ContentType.Application.Json)
                setBody(BatchRequest(ids = ids, comment = comment))
            }.body()
        }
    }

    // ------------------------------------------------------------------
    // Safe call wrapper
    // ------------------------------------------------------------------

    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
        return try {
            val response = apiCall()
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(
                message = e.message ?: "An unknown error occurred"
            )
        }
    }
}
