package com.arun.asap.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arun.asap.core.network.TokenManager
import com.arun.asap.presentation.dashboard.DashboardViewModel
import com.arun.asap.presentation.decisions.DecisionDetailScreen
import com.arun.asap.presentation.login.LoginScreen
import com.arun.asap.presentation.login.LoginViewModel
import com.arun.asap.presentation.main.MainScreen
import com.arun.asap.presentation.notifications.NotificationsViewModel
import com.arun.asap.presentation.splash.SplashScreen

@Composable
fun AppNavHost(
    viewModel: DashboardViewModel,
    notificationsViewModel: NotificationsViewModel,
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    val navController: NavHostController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) }
    ) {
        // ── Splash ──
        composable(route = AppRoute.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    // Skip login if already authenticated
                    val destination = if (TokenManager.isLoggedIn()) {
                        AppRoute.Main.route
                    } else {
                        AppRoute.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(AppRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ──
        composable(route = AppRoute.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(AppRoute.Main.route) {
                        popUpTo(AppRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Main (with bottom nav) ──
        composable(route = AppRoute.Main.route) {
            MainScreen(
                viewModel = viewModel,
                notificationsViewModel = notificationsViewModel,
                onDecisionClick = { erpId ->
                    navController.navigate(AppRoute.DecisionDetail.createRoute(erpId))
                },
                onLogout = {
                    TokenManager.clearToken()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(AppRoute.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Decision detail ──
        composable(
            route = AppRoute.DecisionDetail.route,
            arguments = listOf(navArgument(AppRoute.DecisionDetail.ARG_ERP_ID) { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition = { slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)) }
        ) { backStackEntry ->
            val erpId = backStackEntry.arguments?.getString(AppRoute.DecisionDetail.ARG_ERP_ID)
            DecisionDetailScreen(
                erpId = erpId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
