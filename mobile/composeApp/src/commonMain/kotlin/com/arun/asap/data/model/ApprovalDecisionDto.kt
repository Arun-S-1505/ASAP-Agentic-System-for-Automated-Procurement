package com.arun.asap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data transfer object for approval decision.
 */
@Serializable
data class ApprovalDecisionDto(
    val id: String = "",
    @SerialName("erp_requisition_id")
    val erpRequisitionId: String = "",
    @SerialName("risk_score")
    val riskScore: Double? = null,
    @SerialName("risk_explanation")
    val riskExplanation: String? = null,
    val decision: String = "",
    val state: String = "",
    @SerialName("commit_at")
    val commitAt: String? = null,
    @SerialName("committed_at")
    val committedAt: String? = null,
    val comment: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

