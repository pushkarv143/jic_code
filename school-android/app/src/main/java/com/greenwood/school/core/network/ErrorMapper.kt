package com.greenwood.school.core.network

import android.content.Context
import com.greenwood.school.R
import com.greenwood.school.data.remote.dto.ErrorEnvelope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where a `Throwable` becomes a user-facing [AppError].
 *
 * The backend's `GlobalExceptionHandler` already writes a human-readable `message`
 * for every case it knows about ("Your account is not active…", "Invalid username
 * or password", …). We prefer that message when present — it keeps Android and web
 * saying exactly the same thing — and fall back to our own copy only when the body
 * is missing, unparseable, or a raw 5xx.
 */
@Singleton
class ErrorMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    fun map(throwable: Throwable): AppError = when (throwable) {
        is AppException -> throwable.error
        is HttpException -> mapHttp(throwable)
        is SocketTimeoutException -> AppError.Timeout(string(R.string.error_timeout))
        is UnknownHostException -> AppError.Network(string(R.string.error_network))
        is IOException -> AppError.Network(string(R.string.error_network))
        else -> AppError.Unknown(string(R.string.error_unknown))
    }

    fun mapHttpStatus(status: Int, body: String?): AppError {
        val envelope = body?.let(::parse)
        val serverMessage = envelope?.message?.takeIf { it.isNotBlank() }

        return when (status) {
            400, 422 -> AppError.Validation(
                userMessage = serverMessage ?: string(R.string.error_validation),
                fieldErrors = envelope?.validationErrors.orEmpty(),
            )

            401 -> AppError.Unauthorized(serverMessage ?: string(R.string.error_unauthorized))
            403 -> AppError.Forbidden(serverMessage ?: string(R.string.error_forbidden))
            404 -> AppError.NotFound(serverMessage ?: string(R.string.error_not_found))
            409 -> AppError.Conflict(serverMessage ?: string(R.string.error_conflict))

            // A 5xx message from the server may leak internals; the handler already
            // scrubs it, but we still prefer our own copy for anything unrecognised.
            in 500..599 -> AppError.Server(string(R.string.error_server), status)

            in 400..499 -> AppError.Client(serverMessage ?: string(R.string.error_unknown), status)
            else -> AppError.Unknown(string(R.string.error_unknown))
        }
    }

    private fun mapHttp(exception: HttpException): AppError {
        val body = runCatching { exception.response()?.errorBody()?.string() }.getOrNull()
        return mapHttpStatus(exception.code(), body)
    }

    private fun parse(body: String): ErrorEnvelope? =
        runCatching { json.decodeFromString<ErrorEnvelope>(body) }.getOrNull()

    private fun string(resId: Int): String = context.getString(resId)
}
