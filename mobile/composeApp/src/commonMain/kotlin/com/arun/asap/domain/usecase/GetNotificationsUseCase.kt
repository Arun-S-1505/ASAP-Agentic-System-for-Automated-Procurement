package com.arun.asap.domain.usecase

import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.NotificationListResponse
import com.arun.asap.data.repository.ApprovalRepository

/**
 * Use case for retrieving notification logs.
 *
 * @param repository The approval repository
 */
class GetNotificationsUseCase(
    private val repository: ApprovalRepository
) {

    /**
     * Executes the get notifications operation.
     *
     * @return ApiResult containing NotificationListResponse or error state
     */
    suspend operator fun invoke(): ApiResult<NotificationListResponse> {
        return repository.getNotifications()
    }
}
