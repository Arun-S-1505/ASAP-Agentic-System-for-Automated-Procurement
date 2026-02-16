package com.arun.asap.presentation.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1B2D),
                        Color(0xFF1B2A4A),
                        Color(0xFF0F1B2D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(0xFF00C9A7).copy(alpha = 0.04f)
            for (i in 0..25) {
                val x = size.width * ((i * 37 + 13) % 100) / 100f
                val y = size.height * ((i * 53 + 7) % 100) / 100f
                drawCircle(color = dotColor, radius = (i % 5 + 2).toFloat() * 4, center = Offset(x, y))
            }
        }

        Column(
            modifier = Modifier.alpha(alphaAnim).scale(scaleAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with pulsating ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                // Pulsating outer ring
                Canvas(
                    modifier = Modifier.size(160.dp).scale(pulseScale).alpha(pulseAlpha)
                ) {
                    drawCircle(
                        color = Color(0xFF00C9A7),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Main logo circle
                Canvas(modifier = Modifier.size(120.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2

                    // Outer gradient ring
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00C9A7), Color(0xFF00E5FF))
                        ),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Inner filled disc
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1B3A5C), Color(0xFF0F1B2D))
                        ),
                        radius = radius - 5.dp.toPx(),
                        center = center
                    )
                    // Lightning bolt
                    val s = radius * 0.45f
                    val bolt = Path().apply {
                        moveTo(center.x + s * 0.05f, center.y - s * 0.9f)
                        lineTo(center.x + s * 0.5f, center.y - s * 0.9f)
                        lineTo(center.x + s * 0.08f, center.y + s * 0.05f)
                        lineTo(center.x + s * 0.45f, center.y + s * 0.05f)
                        lineTo(center.x - s * 0.15f, center.y + s * 1.0f)
                        lineTo(center.x + s * 0.05f, center.y + s * 0.05f)
                        lineTo(center.x - s * 0.35f, center.y + s * 0.05f)
                        close()
                    }
                    drawPath(bolt, color = Color(0xFF00C9A7))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ASAP",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Agentic System for Automated Procurement",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF00C9A7).copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading dots
            LoadingDots()
        }

        // Version at bottom
        Text(
            text = "v1.0.0",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).alpha(alphaAnim),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun LoadingDots() {
    val inf = rememberInfiniteTransition(label = "dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val alpha by inf.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$i"
            )
            Canvas(modifier = Modifier.size(8.dp).alpha(alpha)) {
                drawCircle(color = Color(0xFF00C9A7))
            }
        }
    }
}
