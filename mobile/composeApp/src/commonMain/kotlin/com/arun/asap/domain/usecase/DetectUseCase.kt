package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.DetectResponse
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for detecting staged requisitions.
 * Encapsulates the business logic for the detect operation.
 *
 * @param repository The approval repository
 */
class DetectUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Executes the detect operation.
     *
     * @return ApiResult containing DetectResponse or error state
     */
    suspend operator fun invoke(): ApiResult<DetectResponse> {
        return repository.detect()
    }
}

