package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.UndoResponse
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for undoing a decision.
 * Encapsulates the business logic for the undo operation.
 *
 * @param repository The approval repository
 */
class UndoDecisionUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Executes the undo operation for a specific requisition.
     *
     * @param requisitionId The ID of the requisition to undo
     * @return ApiResult containing UndoResponse or error state
     */
    suspend operator fun invoke(requisitionId: String): ApiResult<UndoResponse> {
        return repository.undo(requisitionId)
    }
}

