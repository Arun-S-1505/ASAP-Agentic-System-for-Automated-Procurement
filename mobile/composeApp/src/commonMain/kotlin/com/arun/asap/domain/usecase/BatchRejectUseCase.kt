package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.BatchResponse
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for batch-rejecting multiple decisions at once.
 *
 * @param repository The approval repository
 */
class BatchRejectUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Batch-reject the given requisition IDs.
     *
     * @param ids List of ERP requisition IDs
     * @param comment Optional rejection reason
     * @return ApiResult containing BatchResponse or error state
     */
    suspend operator fun invoke(
        ids: List<String>,
        comment: String? = null
    ): ApiResult<BatchResponse> {
        return repository.batchReject(ids, comment)
    }
}
