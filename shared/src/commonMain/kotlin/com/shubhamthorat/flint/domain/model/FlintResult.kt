package com.shubhamthorat.flint.domain.model

/**
 * Domain Result abstraction for Flint multiplatform clean architecture.
 * Encapsulates typed success data [T] or typed domain error [E].
 */
sealed interface FlintResult<out T, out E : AppError> {

    data class Success<out T>(val data: T) : FlintResult<T, Nothing>

    data class Error<out E : AppError>(val error: E) : FlintResult<Nothing, E>
}

inline fun <T, E : AppError, R> FlintResult<T, E>.map(
    transform: (T) -> R
): FlintResult<R, E> {
    return when (this) {
        is FlintResult.Success -> FlintResult.Success(transform(data))
        is FlintResult.Error -> this
    }
}

inline fun <T, E : AppError, R : AppError> FlintResult<T, E>.mapError(
    transform: (E) -> R
): FlintResult<T, R> {
    return when (this) {
        is FlintResult.Success -> this
        is FlintResult.Error -> FlintResult.Error(transform(error))
    }
}

inline fun <T, E : AppError, R> FlintResult<T, E>.flatMap(
    transform: (T) -> FlintResult<R, E>
): FlintResult<R, E> {
    return when (this) {
        is FlintResult.Success -> transform(data)
        is FlintResult.Error -> this
    }
}

inline fun <T, E : AppError, R> FlintResult<T, E>.fold(
    onSuccess: (T) -> R,
    onError: (E) -> R
): R {
    return when (this) {
        is FlintResult.Success -> onSuccess(data)
        is FlintResult.Error -> onError(error)
    }
}

fun <T, E : AppError> FlintResult<T, E>.getOrNull(): T? {
    return when (this) {
        is FlintResult.Success -> data
        is FlintResult.Error -> null
    }
}

fun <T, E : AppError> FlintResult<T, E>.getOrThrow(): T {
    return when (this) {
        is FlintResult.Success -> data
        is FlintResult.Error -> throw IllegalStateException("FlintResult failed with error: ${error.message}")
    }
}
