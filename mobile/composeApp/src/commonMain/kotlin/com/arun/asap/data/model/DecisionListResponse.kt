package com.arun.asap.data.model

import kotlinx.serialization.Serializable

/**
 * Response model for decisions list API endpoint.
 */
@Serializable
data class DecisionListResponse(
    val decisions: List<ApprovalDecisionDto> = emptyList(),
    val total: Int = 0
)


