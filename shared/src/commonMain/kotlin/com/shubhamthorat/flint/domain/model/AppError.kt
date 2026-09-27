package com.shubhamthorat.flint.domain.model

/**
 * Domain Error taxonomy for Flint.
 * Represents all structured domain errors across network, authentication,
 * AI generation, quota limits, storage, server, and general operations.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class Network(
        override val message: String,
        val code: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Auth(
        override val message: String,
        val errorCode: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class AiProvider(
        override val message: String,
        val provider: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class QuotaExceeded(
        override val message: String,
        val resourceType: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Storage(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Server(
        override val message: String,
        val statusCode: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Validation(
        override val message: String,
        val field: String? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
