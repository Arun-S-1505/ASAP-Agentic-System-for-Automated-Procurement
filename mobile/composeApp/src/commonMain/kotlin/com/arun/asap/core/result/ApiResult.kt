package com.arun.asap.core.result

/**
 * Sealed class representing the result of an API operation.
 */
sealed class ApiResult<out T> {

    /**
     * Successful API response with data.
     */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * Error state with error message.
     */
    data class Error(val message: String) : ApiResult<Nothing>()

    /**
     * Loading state while API request is in progress.
     */
    data object Loading : ApiResult<Nothing>()
}

