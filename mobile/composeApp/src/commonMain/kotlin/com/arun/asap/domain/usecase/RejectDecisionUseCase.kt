package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.ApproveRejectResponse
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for rejecting a pending decision.
 * Encapsulates the business logic for the reject operation.
 *
 * @param repository The approval repository
 */
class RejectDecisionUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Executes the reject operation for a specific requisition.
     *
     * @param requisitionId The ERP requisition ID to reject
     * @param comment Optional rejection reason
     * @return ApiResult containing ApproveRejectResponse or error state
     */
    suspend operator fun invoke(
        requisitionId: String,
        comment: String? = null
    ): ApiResult<ApproveRejectResponse> {
        return repository.reject(requisitionId, comment)
    }
}
