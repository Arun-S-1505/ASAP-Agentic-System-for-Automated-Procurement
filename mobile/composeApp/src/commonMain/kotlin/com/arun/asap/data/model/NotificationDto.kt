package com.arun.asap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data transfer object matching backend's NotificationLogOut schema.
 */
@Serializable
data class NotificationDto(
    val id: Int = 0,
    @SerialName("erp_requisition_id")
    val erpRequisitionId: String = "",
    val decision: String = "",
    val channel: String = "",
    val status: String = "",
    val message: String? = null,
    @SerialName("created_at")
    val createdAt: String = ""
) {
    /**
     * Maps the decision field to a UI notification type.
     */
    val notificationType: String
        get() = when (decision) {
            "auto_approve", "manual_approve" -> "success"
            "reject" -> "alert"
            "hold" -> "alert"
            else -> "info"
        }

    /**
     * Generates a human-readable title from decision + channel.
     */
    val title: String
        get() {
            val decisionLabel = decision.replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            val channelLabel = channel.replaceFirstChar { it.uppercase() }
            return "$decisionLabel — $channelLabel"
        }

    /**
     * Derives a relative time string from createdAt timestamp.
     * Falls back to the raw timestamp if parsing fails.
     */
    val timeAgo: String
        get() = try {
            // Parse ISO timestamp and compute relative time
            val createdParts = createdAt.replace("T", " ").substringBefore(".").split(" ")
            if (createdParts.size >= 2) {
                "${createdParts[0]}  ${createdParts[1]}"
            } else {
                createdAt
            }
        } catch (_: Exception) {
            createdAt
        }
}
