package com.arun.asap.presentation.analytics

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arun.asap.presentation.dashboard.DashboardViewModel
import com.arun.asap.presentation.theme.AsapError
import com.arun.asap.presentation.theme.AsapIndigo
import com.arun.asap.presentation.theme.AsapSlate
import com.arun.asap.presentation.theme.AsapSuccess
import com.arun.asap.presentation.theme.AsapTeal
import com.arun.asap.presentation.theme.AsapWarning

@Composable
fun AnalyticsScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val analytics by viewModel.analytics.collectAsState()
    val analyticsLoading by viewModel.analyticsLoading.collectAsState()

    // Load analytics from backend on first composition
    LaunchedEffect(Unit) { viewModel.loadAnalytics() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AsapIndigo, AsapSlate)))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = AsapTeal, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Procurement insights & trends", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            }
        }

        if (analyticsLoading && analytics == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AsapTeal)
            }
        } else if (analytics == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                Text("No analytics data available", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val data = analytics!!

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Approval Distribution (Donut) ──
                AnalyticsCard(title = "Approval Distribution") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            slices = listOf(
                                DonutSlice(data.autoApproved.toFloat(), AsapSuccess),
                                DonutSlice(data.manualApproved.toFloat(), AsapWarning),
                                DonutSlice(data.held.toFloat(), AsapError),
                            ),
                            modifier = Modifier.size(120.dp),
                            centerText = data.totalDecisions.toString()
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LegendItem(color = AsapSuccess, label = "Auto Approve", value = data.autoApproved)
                            LegendItem(color = AsapWarning, label = "Manual", value = data.manualApproved)
                            LegendItem(color = AsapError, label = "Hold", value = data.held)
                            if (data.rejected > 0) {
                                LegendItem(color = Color(0xFF9C27B0), label = "Rejected", value = data.rejected)
                            }
                        }
                    }
                }

                // ── Risk Distribution (Bars) ──
                AnalyticsCard(title = "Risk Distribution") {
                    val rd = data.riskDistribution
                    val maxVal = maxOf(rd.low, rd.medium, rd.high, 1)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        BarRow("Low Risk", rd.low, maxVal, AsapSuccess)
                        BarRow("Medium Risk", rd.medium, maxVal, AsapWarning)
                        BarRow("High Risk", rd.high, maxVal, AsapError)
                    }
                }

                // ── Daily Trend (Bar chart) ──
                if (data.dailyCounts.isNotEmpty()) {
                    AnalyticsCard(title = "Daily Trend (Last 7 Days)") {
                        val values = data.dailyCounts.map { it.count.toFloat() }
                        val labels = data.dailyCounts.map {
                            // Show last 5 chars of date, e.g. "02-15"
                            if (it.date.length >= 5) it.date.takeLast(5) else it.date
                        }
                        WeeklyBarChart(values = values, labels = labels)
                    }
                }

                // ── Key Metrics ──
                AnalyticsCard(title = "Key Metrics") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricRow("Avg. Risk Score", "%.2f".format(data.avgRiskScore))
                        MetricRow("Automation Rate", "%.1f%%".format(data.automationRate))
                        MetricRow("Total Decisions", data.totalDecisions.toString())
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ═══════════════════════════ Components ═══════════════════════════

@Composable
private fun AnalyticsCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private data class DonutSlice(val value: Float, val color: Color)

@Composable
private fun DonutChart(slices: List<DonutSlice>, modifier: Modifier = Modifier, centerText: String = "") {
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            var startAngle = -90f
            for (slice in slices) {
                val sweep = (slice.value / total) * 360f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Text(
            text = centerText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Text("$label: $value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BarRow(label: String, value: Int, maxValue: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(color.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(values: List<Float>, labels: List<String>) {
    val maxVal = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Column {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(120.dp)
        ) {
            val barCount = values.size
            val spacing = 8.dp.toPx()
            val barWidth = (size.width - spacing * (barCount + 1)) / barCount
            values.forEachIndexed { i, v ->
                val barHeight = (v / maxVal) * size.height * 0.85f
                val x = spacing + i * (barWidth + spacing)
                val y = size.height - barHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF00C9A7), Color(0xFF00E5FF))),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            labels.forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
