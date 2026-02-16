package com.arun.asap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for detect API endpoint.
 */
@Serializable
data class DetectResponse(
    @SerialName("staged_count")
    val stagedCount: Int = 0,
    val message: String = "",
    // Alternative field names that the API might use
    val count: Int? = null,
    val status: String? = null,
    val detail: String? = null
) {
    /**
     * Get the effective staged count from either field
     */
    fun getEffectiveStagedCount(): Int = count ?: stagedCount

    /**
     * Get the effective message from available fields
     */
    fun getEffectiveMessage(): String = when {
        message.isNotBlank() -> message
        detail != null -> detail
        status != null -> status
        else -> "Detection completed"
    }
}


