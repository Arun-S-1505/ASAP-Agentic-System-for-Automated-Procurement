package com.arun.asap.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.asap.core.network.ApiClient
import com.arun.asap.core.network.TokenManager
import com.arun.asap.core.result.ApiResult
import kotlinx.coroutines.launch

/**
 * ViewModel for the Login screen.
 * Manages login/register state and communicates with the backend auth API.
 */
class LoginViewModel(private val apiClient: ApiClient) : ViewModel() {

    var username by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var fullName by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isRegisterMode by mutableStateOf(false)
        private set
    var loginSuccess by mutableStateOf(false)
        private set

    fun onUsernameChange(value: String) { username = value }
    fun onPasswordChange(value: String) { password = value }
    fun onFullNameChange(value: String) { fullName = value }
    fun toggleRegisterMode() {
        isRegisterMode = !isRegisterMode
        errorMessage = null
    }

    fun login() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Please enter username and password"
            return
        }
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            val result = apiClient.login(username, password)
            isLoading = false
            when (result) {
                is ApiResult.Success -> {
                    TokenManager.saveToken(result.data.accessToken)
                    TokenManager.saveUserInfo(
                        username = result.data.user.username,
                        fullName = result.data.user.fullName,
                        role = result.data.user.role
                    )
                    loginSuccess = true
                }
                is ApiResult.Error -> {
                    errorMessage = result.message
                }
                else -> {}
            }
        }
    }

    fun register() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Please enter username and password"
            return
        }
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            val result = apiClient.register(username, password, fullName.ifBlank { username })
            isLoading = false
            when (result) {
                is ApiResult.Success -> {
                    TokenManager.saveToken(result.data.accessToken)
                    TokenManager.saveUserInfo(
                        username = result.data.user.username,
                        fullName = result.data.user.fullName,
                        role = result.data.user.role
                    )
                    loginSuccess = true
                }
                is ApiResult.Error -> {
                    errorMessage = result.message
                }
                else -> {}
            }
        }
    }
}
