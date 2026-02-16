package com.arun.asap.core.network

/**
 * In-memory JWT token manager (singleton).
 *
 * Stores the access token and user info after login and provides them to API calls.
 * In production, this would use encrypted SharedPreferences / DataStore.
 */
object TokenManager {

    private var accessToken: String? = null
    private var _username: String? = null
    private var _fullName: String? = null
    private var _role: String? = null

    /** Save the JWT token after a successful login. */
    fun saveToken(token: String) {
        accessToken = token
    }

    /** Save user profile info alongside the token. */
    fun saveUserInfo(username: String, fullName: String, role: String) {
        _username = username
        _fullName = fullName
        _role = role
    }

    /** Retrieve the current JWT token, or null if not logged in. */
    fun getToken(): String? = accessToken

    /** Retrieve stored username. */
    fun getUsername(): String? = _username

    /** Retrieve stored full name. */
    fun getFullName(): String? = _fullName

    /** Retrieve stored role. */
    fun getRole(): String? = _role

    /** Check if user is currently authenticated. */
    fun isLoggedIn(): Boolean = accessToken != null

    /** Clear all stored data (logout). */
    fun clearToken() {
        accessToken = null
        _username = null
        _fullName = null
        _role = null
    }
}
