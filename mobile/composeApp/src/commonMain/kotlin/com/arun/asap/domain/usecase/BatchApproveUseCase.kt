package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.BatchResponse
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for batch-approving multiple decisions at once.
 *
 * @param repository The approval repository
 */
class BatchApproveUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Batch-approve the given requisition IDs.
     *
     * @param ids List of ERP requisition IDs
     * @param comment Optional approval comment
     * @return ApiResult containing BatchResponse or error state
     */
    suspend operator fun invoke(
        ids: List<String>,
        comment: String? = null
    ): ApiResult<BatchResponse> {
        return repository.batchApprove(ids, comment)
    }
}
