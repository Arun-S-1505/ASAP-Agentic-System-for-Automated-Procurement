package com.arun.asap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for POST /batch/approve and POST /batch/reject. */
@Serializable
data class BatchRequest(
    val ids: List<String>,
    val comment: String? = null
)

/** Individual result for one item in a batch operation. */
@Serializable
data class BatchItemResult(
    @SerialName("erp_requisition_id") val erpRequisitionId: String,
    val success: Boolean,
    val message: String
)

/** Response from POST /batch/approve and POST /batch/reject. */
@Serializable
data class BatchResponse(
    val processed: Int,
    val failed: Int,
    val results: List<BatchItemResult>
)
