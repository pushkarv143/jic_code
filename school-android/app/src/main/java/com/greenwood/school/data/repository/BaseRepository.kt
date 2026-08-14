package com.greenwood.school.data.repository

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.ErrorMapper
import com.greenwood.school.core.network.Paged
import com.greenwood.school.core.network.apiCall
import com.greenwood.school.core.network.map
import com.greenwood.school.core.network.requireData
import com.greenwood.school.core.network.toPaged
import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.PageEnvelope
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Shared plumbing for every repository: run off the main thread, normalise
 * failures into [com.greenwood.school.core.network.AppError], and unwrap the
 * backend's `ApiResponse` / `PageResponse` envelopes.
 *
 * Keeping this in one place is what stops 15 repositories from each inventing
 * their own slightly different try/catch.
 */
abstract class BaseRepository(
    private val dispatcher: CoroutineDispatcher,
    private val errorMapper: ErrorMapper,
) {

    /** Raw call — use when the endpoint returns something other than an envelope. */
    protected suspend fun <T> execute(block: suspend () -> T): ApiResult<T> =
        apiCall(dispatcher, errorMapper, block)

    /** Call returning `ApiResponse<T>`; yields `T`. */
    protected suspend fun <T> call(block: suspend () -> ApiEnvelope<T>): ApiResult<T> =
        apiCall(dispatcher, errorMapper) { block().requireData() }

    /**
     * Call returning `ApiResponse<Void>`-style acknowledgements, or a 204 with no
     * body at all (the delete endpoints).
     */
    protected suspend fun ack(block: suspend () -> Unit): ApiResult<Unit> =
        apiCall(dispatcher, errorMapper) { block() }

    /** Call returning `ApiResponse<PageResponse<T>>`; yields a [Paged]. */
    protected suspend fun <T> paged(block: suspend () -> ApiEnvelope<PageEnvelope<T>>): ApiResult<Paged<T>> =
        call(block).map { it.toPaged() }
}
