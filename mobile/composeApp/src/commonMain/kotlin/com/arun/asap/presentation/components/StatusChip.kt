package com.arun.asap.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatusChip(state: String, modifier: Modifier = Modifier) {
    val (bg, fg) = stateColors(state)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = formatState(state),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

private fun formatState(state: String): String =
    state.replace("_", " ").uppercase()

@Composable
private fun stateColors(state: String): Pair<Color, Color> = when (state.lowercase()) {
    "committed"      -> Color(0xFF00C853).copy(alpha = 0.15f) to Color(0xFF00963F)
    "pending_commit" -> Color(0xFF2196F3).copy(alpha = 0.15f) to Color(0xFF0D47A1)
    "cancelled"      -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    "failed"         -> Color(0xFFE53935).copy(alpha = 0.15f) to Color(0xFFB71C1C)
    else             -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
}
