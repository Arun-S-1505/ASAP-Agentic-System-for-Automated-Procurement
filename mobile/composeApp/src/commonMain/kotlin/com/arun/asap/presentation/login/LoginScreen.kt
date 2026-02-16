package com.arun.asap.presentation.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgDark = Color(0xFF0F1B2D)
private val BgMid = Color(0xFF1B2A4A)
private val Accent = Color(0xFF00C9A7)
private val AccentCyan = Color(0xFF00E5FF)
private val SurfaceCard = Color(0xFF162236)
private val TextDim = Color(0xFF8A9BB5)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(Unit) { startAnimation = true }

    // Navigate on success
    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgDark, BgMid, BgDark)
                )
            )
    ) {
        // Subtle background particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Accent.copy(alpha = 0.03f)
            for (i in 0..20) {
                val x = size.width * ((i * 37 + 13) % 100) / 100f
                val y = size.height * ((i * 53 + 7) % 100) / 100f
                drawCircle(color = dotColor, radius = (i % 5 + 2).toFloat() * 5, center = Offset(x, y))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .alpha(alphaAnim)
                .scale(scaleAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo ──
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2
                    drawCircle(
                        brush = Brush.linearGradient(listOf(Accent, AccentCyan)),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFF1B3A5C), BgDark)),
                        radius = radius - 4.dp.toPx(),
                        center = center
                    )
                    val s = radius * 0.4f
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
                    drawPath(bolt, color = Accent)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ASAP",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (viewModel.isRegisterMode) "Create your account" else "Sign in to continue",
                fontSize = 14.sp,
                color = TextDim
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Register: full name field ──
            AnimatedVisibility(
                visible = viewModel.isRegisterMode,
                enter = fadeIn() + slideInVertically { -it }
            ) {
                Column {
                    OutlinedTextField(
                        value = viewModel.fullName,
                        onValueChange = viewModel::onFullNameChange,
                        label = { Text("Full Name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Accent)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // ── Username ──
            OutlinedTextField(
                value = viewModel.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Accent)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Password ──
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Accent)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password",
                            tint = TextDim
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )

            // ── Error message ──
            AnimatedVisibility(visible = viewModel.errorMessage != null) {
                Text(
                    text = viewModel.errorMessage ?: "",
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Login / Register button ──
            Button(
                onClick = {
                    if (viewModel.isRegisterMode) viewModel.register() else viewModel.login()
                },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = BgDark
                )
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = BgDark
                    )
                } else {
                    Text(
                        text = if (viewModel.isRegisterMode) "Create Account" else "Sign In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Toggle login / register ──
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (viewModel.isRegisterMode) "Already have an account?" else "Don't have an account?",
                    color = TextDim,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (viewModel.isRegisterMode) "Sign In" else "Register",
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { viewModel.toggleRegisterMode() }
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = TextDim.copy(alpha = 0.3f),
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextDim,
    cursorColor = Accent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLeadingIconColor = Accent,
    unfocusedLeadingIconColor = TextDim,
    focusedContainerColor = SurfaceCard,
    unfocusedContainerColor = SurfaceCard
)
