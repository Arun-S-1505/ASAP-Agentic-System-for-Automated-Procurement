package com.arun.asap.presentation.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arun.asap.presentation.theme.AsapError
import com.arun.asap.presentation.theme.AsapIndigo
import com.arun.asap.presentation.theme.AsapSlate
import com.arun.asap.presentation.theme.AsapTeal

@Composable
fun SettingsScreen(
    userName: String = "User",
    userRole: String = "Member",
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var darkMode by remember { mutableStateOf(false) }
    var pushNotifications by remember { mutableStateOf(true) }
    var emailAlerts by remember { mutableStateOf(false) }
    var biometric by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Profile header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AsapIndigo, AsapSlate)))
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(AsapTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(userRole, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Appearance ──
            SettingsSection(title = "Appearance") {
                SettingsToggle(icon = Icons.Filled.DarkMode, title = "Dark Mode", subtitle = "Use dark theme", checked = darkMode, onCheckedChange = { darkMode = it })
                SettingsDivider()
                SettingsNav(icon = Icons.Filled.Language, title = "Language", subtitle = "English (US)")
            }

            // ── Notifications ──
            SettingsSection(title = "Notifications") {
                SettingsToggle(icon = Icons.Filled.Notifications, title = "Push Notifications", subtitle = "Receive push alerts", checked = pushNotifications, onCheckedChange = { pushNotifications = it })
                SettingsDivider()
                SettingsToggle(icon = Icons.Filled.Notifications, title = "Email Alerts", subtitle = "Get email digests", checked = emailAlerts, onCheckedChange = { emailAlerts = it })
            }

            // ── Security ──
            SettingsSection(title = "Security") {
                SettingsToggle(icon = Icons.Filled.Fingerprint, title = "Biometric Login", subtitle = "Use fingerprint or face", checked = biometric, onCheckedChange = { biometric = it })
                SettingsDivider()
                SettingsNav(icon = Icons.Filled.Security, title = "Change Password", subtitle = "Last changed 30 days ago")
            }

            // ── System ──
            SettingsSection(title = "System") {
                SettingsNav(icon = Icons.Filled.Storage, title = "API Endpoint", subtitle = "http://10.0.2.2:8000")
                SettingsDivider()
                SettingsNav(icon = Icons.Filled.Settings, title = "Cache & Data", subtitle = "Clear local cache")
            }

            // ── About ──
            SettingsSection(title = "About") {
                SettingsNav(icon = Icons.Filled.Info, title = "Version", subtitle = "1.0.0 (Build 1)")
                SettingsDivider()
                SettingsNav(icon = Icons.Filled.Info, title = "Terms of Service")
                SettingsDivider()
                SettingsNav(icon = Icons.Filled.Info, title = "Privacy Policy")
            }

            // ── Logout ──
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { onLogout() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = AsapError.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AsapError, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AsapError)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ═══════════════════════════ Helpers ═══════════════════════════

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AsapTeal, checkedThumbColor = Color.White)
        )
    }
}

@Composable
private fun SettingsNav(icon: ImageVector, title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 48.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
