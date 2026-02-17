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
    val createdAt: String? = null,

    // --- Enriched SAP requisition fields ---
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("material_code")
    val materialCode: String? = null,
    @SerialName("material_group")
    val materialGroup: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    @SerialName("unit_price")
    val unitPrice: Double? = null,
    @SerialName("total_amount")
    val totalAmount: Double? = null,
    val currency: String? = null,
    val plant: String? = null,
    @SerialName("company_code")
    val companyCode: String? = null,
    @SerialName("purchasing_group")
    val purchasingGroup: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    val supplier: String? = null,
    @SerialName("release_status")
    val releaseStatus: String? = null,
    @SerialName("processing_status")
    val processingStatus: String? = null,
    @SerialName("is_deleted")
    val isDeleted: Boolean? = null,
    @SerialName("is_closed")
    val isClosed: Boolean? = null,
    @SerialName("creation_date")
    val creationDate: String? = null,
    @SerialName("delivery_date")
    val deliveryDate: String? = null
)

