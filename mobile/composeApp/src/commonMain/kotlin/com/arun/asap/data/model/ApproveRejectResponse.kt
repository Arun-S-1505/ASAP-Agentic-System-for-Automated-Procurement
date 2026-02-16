package com.arun.asap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for approve / reject API endpoints.
 */
@Serializable
data class ApproveRejectResponse(
    @SerialName("erp_requisition_id")
    val erpRequisitionId: String = "",
    val decision: String = "",
    val state: String = "",
    val message: String = "",
    val detail: String? = null
) {
    fun getEffectiveMessage(): String = when {
        message.isNotBlank() -> message
        detail != null -> detail
        else -> "Operation completed"
    }
}
