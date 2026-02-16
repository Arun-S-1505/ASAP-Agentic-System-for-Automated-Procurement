package com.arun.asap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arun.asap.di.AppModule
import com.arun.asap.navigation.AppNavHost
import com.arun.asap.presentation.dashboard.DashboardViewModel
import com.arun.asap.presentation.theme.AsapTheme

@Composable
fun App() {
    AsapTheme {
        val viewModel = remember {
            DashboardViewModel(
                detectUseCase = AppModule.detectUseCase,
                getDecisionsUseCase = AppModule.getDecisionsUseCase,
                undoDecisionUseCase = AppModule.undoDecisionUseCase,
                approveDecisionUseCase = AppModule.approveDecisionUseCase,
                rejectDecisionUseCase = AppModule.rejectDecisionUseCase,
                getAnalyticsUseCase = AppModule.getAnalyticsUseCase,
                batchApproveUseCase = AppModule.batchApproveUseCase,
                batchRejectUseCase = AppModule.batchRejectUseCase
            )
        }
        AppNavHost(
            viewModel = viewModel,
            notificationsViewModel = AppModule.notificationsViewModel,
            loginViewModel = AppModule.loginViewModel
        )
    }
}
