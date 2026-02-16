package com.arun.asap.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.NotificationDto
import com.arun.asap.domain.usecase.GetNotificationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Notifications screen.
 * Manages notification list state and loads data from the backend API.
 *
 * @param getNotificationsUseCase Use case for retrieving notifications
 */
class NotificationsViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationDto>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val notifications: StateFlow<List<NotificationDto>> = _notifications.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Loads notifications from the backend API.
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = getNotificationsUseCase()) {
                is ApiResult.Success -> {
                    _notifications.value = result.data.notifications
                    _errorMessage.value = null
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                }
                is ApiResult.Loading -> {
                    // Loading state already set
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
