package com.arun.asap.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AsapLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showText: Boolean = true,
    primaryColor: Color = Color(0xFF00C9A7),
    secondaryColor: Color = Color(0xFF0F1B2D)
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = this.size.minDimension / 2

            // Teal ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f)),
                    start = Offset.Zero,
                    end = Offset(this.size.width, this.size.height)
                ),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            // Inner filled disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1B3A5C), secondaryColor)
                ),
                radius = radius - 6.dp.toPx(),
                center = center
            )
            // Lightning bolt
            val s = radius * 0.4f
            val bolt = Path().apply {
                moveTo(center.x + s * 0.05f, center.y - s * 0.95f)
                lineTo(center.x + s * 0.55f, center.y - s * 0.95f)
                lineTo(center.x + s * 0.08f, center.y + s * 0.05f)
                lineTo(center.x + s * 0.48f, center.y + s * 0.05f)
                lineTo(center.x - s * 0.15f, center.y + s * 1.05f)
                lineTo(center.x + s * 0.05f, center.y + s * 0.1f)
                lineTo(center.x - s * 0.35f, center.y + s * 0.1f)
                close()
            }
            drawPath(bolt, color = primaryColor)
        }

        if (showText) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ASAP",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    letterSpacing = 8.sp
                ),
                color = Color.White
            )
            Text(
                text = "Automated Procurement",
                style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 2.sp),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
