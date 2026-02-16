package com.arun.asap.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arun.asap.data.model.ApprovalDecisionDto
import com.arun.asap.presentation.components.DecisionCard
import com.arun.asap.presentation.components.ShimmerLoadingList
import com.arun.asap.presentation.components.SummaryCard
import com.arun.asap.presentation.theme.AsapError
import com.arun.asap.presentation.theme.AsapIndigo
import com.arun.asap.presentation.theme.AsapSlate
import com.arun.asap.presentation.theme.AsapSuccess
import com.arun.asap.presentation.theme.AsapTeal
import com.arun.asap.presentation.theme.AsapWarning

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onDecisionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val decisions by viewModel.decisions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Committed", "Auto Approve", "Hold")

    val filteredDecisions = remember(decisions, searchQuery, selectedFilter) {
        decisions.filter { d ->
            val matchSearch = searchQuery.isBlank() ||
                d.erpRequisitionId.contains(searchQuery, ignoreCase = true) ||
                d.decision.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedFilter) {
                "Pending" -> d.state == "pending_commit" || d.state == "detected"
                "Committed" -> d.state == "committed"
                "Auto Approve" -> d.decision == "auto_approve"
                "Hold" -> d.decision == "hold"
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    LaunchedEffect(Unit) { viewModel.loadDecisions() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSuccess()
        }
    }

    val inSelectionMode = selectedIds.isNotEmpty()

    val total = decisions.size
    val pending = decisions.count { it.state == "pending_commit" }
    val committed = decisions.count { it.state == "committed" }
    val autoApproved = decisions.count { it.decision == "auto_approve" }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading && decisions.isEmpty() -> ShimmerLoadingList()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // ── Gradient header ──
                        item {
                            GradientHeader(
                                isLoading = isLoading,
                                onDetect = { viewModel.detect() },
                                onRefresh = { viewModel.loadDecisions() }
                            )
                        }

                        // ── Search bar ──
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                placeholder = {
                                    Text("Search decisions...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Search, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AsapTeal,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                )
                            )
                        }

                        // ── KPI cards ──
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SummaryCard(
                                        title = "Total",
                                        value = total.toString(),
                                        color = Color(0xFF2196F3),
                                        icon = Icons.Filled.Inventory2,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SummaryCard(
                                        title = "Pending",
                                        value = pending.toString(),
                                        color = AsapWarning,
                                        icon = Icons.Filled.Schedule,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SummaryCard(
                                        title = "Committed",
                                        value = committed.toString(),
                                        color = AsapSuccess,
                                        icon = Icons.Filled.CheckCircle,
                                        modifier = Modifier.weight(1f)
                                    )
                                    SummaryCard(
                                        title = "Auto Approved",
                                        value = autoApproved.toString(),
                                        color = Color(0xFF9C27B0),
                                        icon = Icons.Filled.DoneAll,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ── Filter chips ──
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filters) { label ->
                                    FilterChip(
                                        selected = selectedFilter == label,
                                        onClick = { selectedFilter = label },
                                        label = {
                                            Text(label, style = MaterialTheme.typography.labelMedium)
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AsapTeal.copy(alpha = 0.15f),
                                            selectedLabelColor = AsapTeal,
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // ── Decision list ──
                        if (filteredDecisions.isEmpty() && !isLoading) {
                            item {
                                EmptyState(
                                    hasDecisions = decisions.isNotEmpty(),
                                    onDetect = { viewModel.detect() }
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = "Recent Decisions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            items(items = filteredDecisions, key = { it.id }) { decision ->
                                val isSelected = selectedIds.contains(decision.erpRequisitionId)
                                val borderMod = if (isSelected) {
                                    Modifier.border(2.dp, AsapTeal, RoundedCornerShape(16.dp))
                                } else Modifier

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .then(borderMod)
                                        .clip(RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = {
                                                viewModel.toggleSelect(decision.erpRequisitionId)
                                            }
                                        )
                                ) {
                                    DecisionCard(
                                        decision = decision,
                                        onUndo = { viewModel.undo(it) },
                                        onClick = { erpId ->
                                            if (inSelectionMode) {
                                                viewModel.toggleSelect(erpId)
                                            } else {
                                                onDecisionClick(erpId)
                                            }
                                        },
                                        modifier = Modifier
                                    )
                                    // Selection check overlay
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(AsapTeal),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }

            if (isLoading && decisions.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = AsapTeal
                )
            }

            // ── Batch action bar ──
            AnimatedVisibility(
                visible = inSelectionMode,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.batchApprove() },
                            colors = ButtonDefaults.buttonColors(containerColor = AsapSuccess),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve")
                        }
                        Button(
                            onClick = { viewModel.batchReject() },
                            colors = ButtonDefaults.buttonColors(containerColor = AsapError),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════ Sub-composables ═══════════════════════════

@Composable
private fun GradientHeader(
    isLoading: Boolean,
    onDetect: () -> Unit,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(AsapIndigo, AsapSlate)))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back! \uD83D\uDC4B",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage your procurement approvals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(AsapTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    text = "Detect",
                    icon = Icons.Filled.PlayArrow,
                    color = AsapTeal,
                    onClick = onDetect,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    text = "Refresh",
                    icon = Icons.Filled.Refresh,
                    color = Color.White.copy(alpha = 0.8f),
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState(hasDecisions: Boolean, onDetect: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = if (hasDecisions) "\uD83D\uDD0D" else "\uD83D\uDCCB", fontSize = 48.sp)
        Text(
            text = if (hasDecisions) "No matching decisions" else "No Decisions Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (hasDecisions) "Try adjusting your search or filters."
                   else "Tap Detect to scan for new requisitions awaiting approval.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
