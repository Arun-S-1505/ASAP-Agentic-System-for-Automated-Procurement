package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.DecisionListResponse
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for retrieving approval decisions.
 * Encapsulates the business logic for fetching the decisions list.
 *
 * @param repository The approval repository
 */
class GetDecisionsUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Executes the get decisions operation.
     *
     * @return ApiResult containing DecisionListResponse or error state
     */
    suspend operator fun invoke(): ApiResult<DecisionListResponse> {
        return repository.getDecisions()
    }
}

