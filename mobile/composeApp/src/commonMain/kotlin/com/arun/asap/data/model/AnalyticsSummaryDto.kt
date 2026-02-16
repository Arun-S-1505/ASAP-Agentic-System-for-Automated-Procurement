package com.arun.asap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Daily count entry for the analytics trend. */
@Serializable
data class DailyCountDto(
    val date: String,
    val count: Int
)

/** Risk distribution breakdown. */
@Serializable
data class RiskDistributionDto(
    val low: Int,
    val medium: Int,
    val high: Int
)

/** Response from GET /analytics/summary. */
@Serializable
data class AnalyticsSummaryDto(
    @SerialName("total_decisions") val totalDecisions: Int,
    @SerialName("auto_approved") val autoApproved: Int,
    @SerialName("manual_approved") val manualApproved: Int,
    val held: Int,
    val rejected: Int,
    @SerialName("avg_risk_score") val avgRiskScore: Double,
    @SerialName("automation_rate") val automationRate: Double,
    @SerialName("risk_distribution") val riskDistribution: RiskDistributionDto,
    @SerialName("daily_counts") val dailyCounts: List<DailyCountDto>
)
