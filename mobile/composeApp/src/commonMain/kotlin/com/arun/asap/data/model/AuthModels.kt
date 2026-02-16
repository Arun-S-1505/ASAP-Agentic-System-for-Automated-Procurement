package com.arun.asap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for POST /auth/login */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/** Request body for POST /auth/register */
@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    @SerialName("full_name") val fullName: String = ""
)

/** Nested user info in auth responses */
@Serializable
data class UserResponse(
    val username: String,
    @SerialName("full_name") val fullName: String,
    val role: String
)

/** Response from POST /auth/login and POST /auth/register */
@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserResponse
)
