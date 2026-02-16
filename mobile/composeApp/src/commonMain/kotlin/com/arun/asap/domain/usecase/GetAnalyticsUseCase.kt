package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.AnalyticsSummaryDto
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for retrieving analytics summary from the backend.
 *
 * @param repository The approval repository
 */
class GetAnalyticsUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Fetches the aggregated analytics summary.
     *
     * @return ApiResult containing AnalyticsSummaryDto or error state
     */
    suspend operator fun invoke(): ApiResult<AnalyticsSummaryDto> {
        return repository.getAnalytics()
    }
}
