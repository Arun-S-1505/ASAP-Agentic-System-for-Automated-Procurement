package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.ApproveRejectResponse
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for approving a pending decision.
 * Encapsulates the business logic for the approve operation.
 *
 * @param repository The approval repository
 */
class ApproveDecisionUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Executes the approve operation for a specific requisition.
     *
     * @param requisitionId The ERP requisition ID to approve
     * @param comment Optional approval comment
     * @return ApiResult containing ApproveRejectResponse or error state
     */
    suspend operator fun invoke(
        requisitionId: String,
        comment: String? = null
    ): ApiResult<ApproveRejectResponse> {
        return repository.approve(requisitionId, comment)
    }
}
