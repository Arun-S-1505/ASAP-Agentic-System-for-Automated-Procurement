package com.arun.asap.data.repository

import com.arun.asap.core.network.ApiClient
import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.AnalyticsSummaryDto
import com.arun.asap.data.model.ApproveRejectResponse
import com.arun.asap.data.model.BatchResponse
import com.arun.asap.data.model.DecisionListResponse
import com.arun.asap.data.model.DetectResponse
import com.arun.asap.data.model.NotificationListResponse
import com.arun.asap.data.model.UndoResponse

/**
 * Repository for approval-related operations.
 * Acts as a single source of truth for approval data by delegating to ApiClient.
 *
 * @param apiClient The API client for network operations
 */
class ApprovalRepository(
    private val apiClient: ApiClient
) {

    /**
     * Detects staged requisitions.
     *
     * @return ApiResult containing DetectResponse or error state
     */
    suspend fun detect(): ApiResult<DetectResponse> {
        return apiClient.detect()
    }

    /**
     * Retrieves the list of approval decisions.
     *
     * @return ApiResult containing DecisionListResponse or error state
     */
    suspend fun getDecisions(): ApiResult<DecisionListResponse> {
        return apiClient.getDecisions()
    }

    /**
     * Undoes a requisition by its ID.
     *
     * @param id The requisition ID to undo
     * @return ApiResult containing UndoResponse or error state
     */
    suspend fun undo(id: String): ApiResult<UndoResponse> {
        return apiClient.undo(id)
    }

    /**
     * Approves a pending requisition.
     *
     * @param id The ERP requisition ID to approve
     * @param comment Optional approval comment
     * @return ApiResult containing ApproveRejectResponse or error state
     */
    suspend fun approve(id: String, comment: String? = null): ApiResult<ApproveRejectResponse> {
        return apiClient.approve(id, comment)
    }

    /**
     * Rejects a pending requisition.
     *
     * @param id The ERP requisition ID to reject
     * @param comment Optional rejection reason
     * @return ApiResult containing ApproveRejectResponse or error state
     */
    suspend fun reject(id: String, comment: String? = null): ApiResult<ApproveRejectResponse> {
        return apiClient.reject(id, comment)
    }

    /**
     * Retrieves notification logs.
     *
     * @return ApiResult containing NotificationListResponse or error state
     */
    suspend fun getNotifications(): ApiResult<NotificationListResponse> {
        return apiClient.getNotifications()
    }

    /**
     * Retrieves aggregated analytics from the backend.
     *
     * @return ApiResult containing AnalyticsSummaryDto or error state
     */
    suspend fun getAnalytics(): ApiResult<AnalyticsSummaryDto> {
        return apiClient.getAnalytics()
    }

    /**
     * Batch-approve multiple requisitions.
     *
     * @param ids List of ERP requisition IDs to approve
     * @param comment Optional approval comment
     * @return ApiResult containing BatchResponse or error state
     */
    suspend fun batchApprove(ids: List<String>, comment: String? = null): ApiResult<BatchResponse> {
        return apiClient.batchApprove(ids, comment)
    }

    /**
     * Batch-reject multiple requisitions.
     *
     * @param ids List of ERP requisition IDs to reject
     * @param comment Optional rejection reason
     * @return ApiResult containing BatchResponse or error state
     */
    suspend fun batchReject(ids: List<String>, comment: String? = null): ApiResult<BatchResponse> {
        return apiClient.batchReject(ids, comment)
    }
}
