package com.arun.asap.data.model

import kotlinx.serialization.Serializable

/**
 * Request body for approve / reject API endpoints.
 */
@Serializable
data class ApproveRejectRequest(
    val comment: String? = null
)
