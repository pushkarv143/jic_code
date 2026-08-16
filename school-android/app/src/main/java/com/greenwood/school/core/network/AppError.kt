package com.greenwood.school.core.network

/**
 * Every failure the app can surface, normalised into one closed set.
 *
 * ViewModels branch on this; UI renders [userMessage]. Raw exceptions and server
 * stack traces never reach the presentation layer.
 */
sealed interface AppError {

    /** Message safe to show a user verbatim. */
    val userMessage: String

    /** No usable network interface. */
    data class Network(override val userMessage: String) : AppError

    /** Connect/read/write timeout. */
    data class Timeout(override val userMessage: String) : AppError

    /** 401 — token missing, invalid, or refresh failed. Triggers a forced sign-out. */
    data class Unauthorized(override val userMessage: String) : AppError

    /** 403 — authenticated but the role lacks the permission. */
    data class Forbidden(override val userMessage: String) : AppError

    /** 404 */
    data class NotFound(override val userMessage: String) : AppError

    /** 409 — duplicate/`DataIntegrityViolation`. */
    data class Conflict(override val userMessage: String) : AppError

    /**
     * 400/422 with a per-field breakdown from `MethodArgumentNotValidException`.
     * [fieldErrors] keys are the DTO property names the backend validated.
     */
    data class Validation(
        override val userMessage: String,
        val fieldErrors: Map<String, String> = emptyMap(),
    ) : AppError

    /**
     * 429 — the caller is going too fast. Distinct from [Client] because it is the
     * one 4xx that means "this would have worked, try again shortly", so a screen
     * can disable its retry rather than presenting the failure as a dead end.
     * Raised by the OTP endpoints, which throttle both sends and guesses.
     */
    data class RateLimited(override val userMessage: String) : AppError

    /** Any other 4xx the app has no specific handling for. */
    data class Client(override val userMessage: String, val status: Int) : AppError

    /** 5xx */
    data class Server(override val userMessage: String, val status: Int) : AppError

    /** Malformed body, serialization failure, or anything genuinely unexpected. */
    data class Unknown(override val userMessage: String) : AppError
}

/**
 * Carrier that lets an [AppError] travel through code that can only throw.
 *
 * [AppError] is deliberately a plain sealed interface rather than a Throwable
 * subclass: it is a value the UI renders, not a control-flow mechanism. Where a
 * throw is unavoidable (unwrapping an envelope inside `apiCall`), wrap it here —
 * `ErrorMapper` unwraps it back out again without losing the original case.
 */
class AppException(val error: AppError) : Exception(error.userMessage)

/** True when the session is no longer usable and the user must sign in again. */
val AppError.isAuthExpiry: Boolean
    get() = this is AppError.Unauthorized

/** True when a retry has a realistic chance of succeeding. */
val AppError.isRetryable: Boolean
    get() = this is AppError.Network || this is AppError.Timeout || this is AppError.Server
