package com.arun.asap.presentation.decisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arun.asap.data.model.ApprovalDecisionDto
import com.arun.asap.presentation.components.RiskGauge
import com.arun.asap.presentation.components.StatusChip
import com.arun.asap.presentation.dashboard.DashboardViewModel
import com.arun.asap.presentation.theme.AsapError
import com.arun.asap.presentation.theme.AsapIndigo
import com.arun.asap.presentation.theme.AsapSlate
import com.arun.asap.presentation.theme.AsapSuccess
import com.arun.asap.presentation.theme.AsapTeal
import com.arun.asap.presentation.theme.AsapWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionDetailScreen(
    erpId: String?,
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val decisions by viewModel.decisions.collectAsState()
    val decision = decisions.find { it.erpRequisitionId == erpId }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Decision Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AsapIndigo,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (decision == null) {
            DecisionNotFound(modifier = Modifier.padding(padding))
        } else {
            DetailContent(
                decision = decision,
                onApprove = { viewModel.approve(it) },
                onReject = { viewModel.reject(it) },
                onUndo = { viewModel.undo(it) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DetailContent(
    decision: ApprovalDecisionDto,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onUndo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AsapIndigo, AsapSlate)))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ERP Requisition", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                Text(
                    text = decision.erpRequisitionId,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DecisionPill(decision.decision)
                    StatusChip(state = decision.state)
                }
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Item Details ──
            SectionCard(icon = Icons.Filled.Info, title = "Item Details") {
                DetailRow("Purchase Requisition", decision.erpRequisitionId)
                decision.productName?.let { DetailRow("Product Name", it) }
                decision.materialCode?.let { DetailRow("Material Code", it) }
                decision.materialGroup?.let { DetailRow("Material Group", it) }
            }

            // ── Pricing ──
            SectionCard(icon = Icons.Filled.Info, title = "Pricing") {
                decision.quantity?.let { DetailRow("Quantity", formatNum(it) + (decision.unit?.let { u -> " $u" } ?: "")) }
                decision.unitPrice?.let { DetailRow("Unit Price", "${decision.currency ?: "USD"} ${formatNum(it)}") }
                decision.totalAmount?.let { DetailRow("Total Amount", "${decision.currency ?: "USD"} ${formatNum(it)}") }
                decision.currency?.let { DetailRow("Currency", it) }
            }

            // ── Organisation ──
            SectionCard(icon = Icons.Filled.Info, title = "Organisation") {
                decision.plant?.let { DetailRow("Plant", it) }
                decision.companyCode?.let { DetailRow("Company Code", it) }
                decision.purchasingGroup?.let { DetailRow("Purchasing Group", it) }
            }

            // ── People ──
            SectionCard(icon = Icons.Filled.Info, title = "People") {
                decision.createdBy?.let { DetailRow("Created By", it) }
                decision.supplier?.let { DetailRow("Supplier", it) }
            }

            // ── Status Flags ──
            SectionCard(icon = Icons.Filled.Shield, title = "Status") {
                decision.releaseStatus?.let { DetailRow("Release Status", releaseStatusText(it)) }
                decision.processingStatus?.let { DetailRow("Processing Status", it) }
                decision.isDeleted?.let { DetailRow("Marked for Deletion", if (it) "Yes" else "No", if (it) AsapError else AsapSuccess) }
                decision.isClosed?.let { DetailRow("Closed", if (it) "Yes" else "No") }
            }

            // ── Dates ──
            SectionCard(icon = Icons.Filled.AccessTime, title = "Dates") {
                decision.creationDate?.let { DetailRow("Creation Date", it) }
                decision.deliveryDate?.let { DetailRow("Delivery Date", it) }
                decision.createdAt?.let { DetailRow("System Created", formatTimestamp(it)) }
                decision.commitAt?.let { DetailRow("Scheduled Commit", formatTimestamp(it)) }
                decision.committedAt?.let { DetailRow("Committed At", formatTimestamp(it)) }
            }

            // ── Risk Assessment ──
            decision.riskScore?.let { score ->
                SectionCard(
                    icon = Icons.Filled.Shield,
                    title = "Risk Assessment",
                    iconTint = riskColor(score)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Risk Score", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(riskLabel(score), style = MaterialTheme.typography.bodyMedium, color = riskColor(score))
                        }
                        RiskGauge(score = score, gaugeSize = 100f)
                    }

                    decision.riskExplanation?.let { explanation ->
                        if (explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = riskColor(score).copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Decision Info ──
            SectionCard(icon = Icons.Filled.Info, title = "Decision Information") {
                DetailRow("Decision Type", formatDecisionText(decision.decision), decisionColor(decision.decision))
                DetailRow("Decision ID", decision.id)
            }

            // ── Comment ──
            decision.comment?.let { comment ->
                if (comment.isNotBlank()) {
                    SectionCard(icon = Icons.AutoMirrored.Filled.Comment, title = "Comments") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Approve / Reject Actions ──
            if (decision.state == "pending_commit" || decision.state == "detected") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Approve Button
                    Button(
                        onClick = { onApprove(decision.erpRequisitionId) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AsapSuccess,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve", fontWeight = FontWeight.SemiBold)
                    }

                    // Reject Button
                    FilledTonalButton(
                        onClick = { onReject(decision.erpRequisitionId) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AsapError.copy(alpha = 0.12f),
                            contentColor = AsapError
                        )
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reject", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Undo (secondary action)
                FilledTonalButton(
                    onClick = { onUndo(decision.erpRequisitionId) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Undo Decision", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ═══════════════════════════ Helpers ═══════════════════════════

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DecisionPill(decision: String) {
    val bg = when (decision) {
        "auto_approve" -> AsapSuccess
        "manual_approve" -> AsapWarning
        "hold" -> AsapError
        else -> AsapTeal
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(
            text = formatDecisionText(decision),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun DecisionNotFound(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("\u26A0\uFE0F", fontSize = 48.sp)
            Text("Decision Not Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "The requested decision could not be found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDecisionText(d: String): String =
    d.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

@Composable
private fun decisionColor(d: String): Color = when (d) {
    "auto_approve" -> AsapSuccess
    "manual_approve" -> AsapWarning
    "hold" -> AsapError
    else -> MaterialTheme.colorScheme.onSurface
}

private fun riskColor(s: Double): Color = when {
    s < 0.3 -> Color(0xFF00C853)
    s < 0.7 -> Color(0xFFFFB300)
    else -> Color(0xFFE53935)
}

private fun riskLabel(s: Double): String = when {
    s < 0.3 -> "Low Risk"
    s < 0.7 -> "Medium Risk"
    else -> "High Risk"
}

private fun formatTimestamp(ts: String): String =
    ts.replace("T", "  ").substringBefore(".")

private fun releaseStatusText(code: String): String = when (code) {
    "01" -> "Released"
    "02" -> "Pending"
    "" -> "Not Released"
    else -> "Status $code"
}

private fun formatNum(v: Double): String {
    val l = v.toLong()
    return if (v == l.toDouble()) l.toString() else "%.2f".format(v)
}
