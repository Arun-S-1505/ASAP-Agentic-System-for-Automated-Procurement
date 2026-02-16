package com.arun.asap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for undo API endpoint.
 */
@Serializable
data class UndoResponse(
    @SerialName("erp_requisition_id")
    val erpRequisitionId: String = "",
    val state: String = "",
    val message: String = "",
    val detail: String? = null,
    val status: String? = null
) {
    fun getEffectiveMessage(): String = when {
        message.isNotBlank() -> message
        detail != null -> detail
        status != null -> status
        else -> "Operation completed"
    }
}


