package com.arun.asap.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arun.asap.core.network.TokenManager
import com.arun.asap.presentation.analytics.AnalyticsScreen
import com.arun.asap.presentation.dashboard.DashboardScreen
import com.arun.asap.presentation.dashboard.DashboardViewModel
import com.arun.asap.presentation.notifications.NotificationsScreen
import com.arun.asap.presentation.notifications.NotificationsViewModel
import com.arun.asap.presentation.settings.SettingsScreen

private data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badge: Int = 0
)

@Composable
fun MainScreen(
    viewModel: DashboardViewModel,
    notificationsViewModel: NotificationsViewModel,
    onDecisionClick: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    val items = listOf(
        NavItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavItem("Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
        NavItem("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, badge = 3),
        NavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            val icon = if (selectedTab == index) item.selectedIcon else item.unselectedIcon
                            if (item.badge > 0 && selectedTab != index) {
                                BadgedBox(badge = { Badge { Text(item.badge.toString()) } }) {
                                    Icon(imageVector = icon, contentDescription = item.label)
                                }
                            } else {
                                Icon(imageVector = icon, contentDescription = item.label)
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel = viewModel, onDecisionClick = onDecisionClick)
                1 -> AnalyticsScreen(viewModel = viewModel)
                2 -> NotificationsScreen(viewModel = notificationsViewModel)
                3 -> SettingsScreen(
                    userName = TokenManager.getFullName() ?: TokenManager.getUsername() ?: "User",
                    userRole = TokenManager.getRole() ?: "Member",
                    onLogout = onLogout
                )
            }
        }
    }
}
