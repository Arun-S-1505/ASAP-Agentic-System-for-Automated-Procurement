package com.arun.asap.core.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory for creating configured HttpClient instances.
 * Provides a centralized configuration for network requests across the application.
 */
object HttpClientFactory {

    /**
     * Creates and configures an HttpClient instance with:
     * - Content negotiation for JSON serialization/deserialization
     * - JSON configuration with ignoreUnknownKeys and prettyPrint settings
     *
     * @return Configured HttpClient instance
     */
    fun createHttpClient(): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = false
                        isLenient = true
                        encodeDefaults = true
                        coerceInputValues = true
                        explicitNulls = false
                    }
                )
            }
        }
    }
}

