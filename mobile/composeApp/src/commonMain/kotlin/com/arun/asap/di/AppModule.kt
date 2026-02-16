package com.arun.asap.di

import com.arun.asap.core.network.ApiClient
import com.arun.asap.core.network.HttpClientFactory
import com.arun.asap.data.repository.ApprovalRepository
import com.arun.asap.domain.usecase.ApproveDecisionUseCase
import com.arun.asap.domain.usecase.BatchApproveUseCase
import com.arun.asap.domain.usecase.BatchRejectUseCase
import com.arun.asap.domain.usecase.DetectUseCase
import com.arun.asap.domain.usecase.GetAnalyticsUseCase
import com.arun.asap.domain.usecase.GetDecisionsUseCase
import com.arun.asap.domain.usecase.GetNotificationsUseCase
import com.arun.asap.domain.usecase.RejectDecisionUseCase
import com.arun.asap.domain.usecase.UndoDecisionUseCase
import com.arun.asap.presentation.login.LoginViewModel
import com.arun.asap.presentation.notifications.NotificationsViewModel
import io.ktor.client.*

/**
 * Simple object-based dependency injection module.
 * Provides singleton instances of app dependencies.
 * Multiplatform safe - no platform-specific code.
 */
object AppModule {

    /**
     * Base URL for API endpoints.
     * IMPORTANT: For real devices, use your computer's IP address (e.g., 192.168.x.x)
     * For emulator: use 10.0.2.2
     * For physical phone: use your computer's actual IP on the same WiFi network
     *
     * For Render cloud deployment, replace with:
     *   "https://<your-render-app>.onrender.com/api/v1"
     */
    private const val BASE_URL = "https://asap-agentic-system-for-automated.onrender.com/api/v1"

    val httpClient: HttpClient by lazy {
        HttpClientFactory.createHttpClient()
    }

    val apiClient: ApiClient by lazy {
        ApiClient(httpClient = httpClient, baseUrl = BASE_URL)
    }

    val approvalRepository: ApprovalRepository by lazy {
        ApprovalRepository(apiClient)
    }

    val detectUseCase: DetectUseCase by lazy {
        DetectUseCase(approvalRepository)
    }

    val getDecisionsUseCase: GetDecisionsUseCase by lazy {
        GetDecisionsUseCase(approvalRepository)
    }

    val undoDecisionUseCase: UndoDecisionUseCase by lazy {
        UndoDecisionUseCase(approvalRepository)
    }

    val approveDecisionUseCase: ApproveDecisionUseCase by lazy {
        ApproveDecisionUseCase(approvalRepository)
    }

    val rejectDecisionUseCase: RejectDecisionUseCase by lazy {
        RejectDecisionUseCase(approvalRepository)
    }

    val getAnalyticsUseCase: GetAnalyticsUseCase by lazy {
        GetAnalyticsUseCase(approvalRepository)
    }

    val batchApproveUseCase: BatchApproveUseCase by lazy {
        BatchApproveUseCase(approvalRepository)
    }

    val batchRejectUseCase: BatchRejectUseCase by lazy {
        BatchRejectUseCase(approvalRepository)
    }

    val getNotificationsUseCase: GetNotificationsUseCase by lazy {
        GetNotificationsUseCase(approvalRepository)
    }

    val notificationsViewModel: NotificationsViewModel by lazy {
        NotificationsViewModel(getNotificationsUseCase)
    }

    val loginViewModel: LoginViewModel by lazy {
        LoginViewModel(apiClient)
    }
}
