package com.arun.asap.data.model

import kotlinx.serialization.Serializable

/**
 * Response wrapper for GET /notifications endpoint.
 */
@Serializable
data class NotificationListResponse(
    val notifications: List<NotificationDto> = emptyList(),
    val total: Int = 0
)
