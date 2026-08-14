package com.greenwood.school.core.network

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.PageEnvelope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Success/failure result of one API interaction. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> = also {
    if (this is ApiResult.Success) action(data)
}

inline fun <T> ApiResult<T>.onFailure(action: (AppError) -> Unit): ApiResult<T> = also {
    if (this is ApiResult.Failure) action(error)
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

fun <T> ApiResult<T>.errorOrNull(): AppError? = (this as? ApiResult.Failure)?.error

/**
 * Runs a suspending API call off the main thread and normalises every throw into
 * an [AppError]. Repositories use this instead of try/catch so error handling is
 * identical everywhere.
 */
suspend inline fun <T> apiCall(
    dispatcher: CoroutineDispatcher,
    errorMapper: ErrorMapper,
    crossinline block: suspend () -> T,
): ApiResult<T> = withContext(dispatcher) {
    try {
        ApiResult.Success(block())
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        // Never swallow cancellation — it is control flow, not an error.
        throw cancellation
    } catch (throwable: Throwable) {
        ApiResult.Failure(errorMapper.map(throwable))
    }
}

/**
 * Unwraps the backend's success envelope.
 *
 * Endpoints that legitimately return no body (`ApiResponse.success(message)`) must
 * be declared as `ApiEnvelope<Unit>` and unwrapped with [requireEnvelopeAck] instead.
 */
fun <T> ApiEnvelope<T>.requireData(): T = data ?: throw AppException(
    AppError.Unknown("The server returned an empty response for a request that expects data."),
)

/** Unwraps an acknowledgement-only envelope, honouring the server's `success` flag. */
fun ApiEnvelope<*>.requireEnvelopeAck() {
    if (!success) {
        throw AppException(
            AppError.Unknown(message.ifBlank { "The request was rejected by the server." }),
        )
    }
}

/** A page plus the cursor a caller needs to decide whether to load more. */
data class Paged<T>(
    val items: List<T>,
    val page: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    companion object {
        fun <T> empty(): Paged<T> = Paged(emptyList(), 0, 0, 0, true)
    }
}

fun <T> PageEnvelope<T>.toPaged(): Paged<T> = Paged(
    items = content,
    page = pageNumber,
    totalElements = totalElements,
    totalPages = totalPages,
    isLast = last,
)
