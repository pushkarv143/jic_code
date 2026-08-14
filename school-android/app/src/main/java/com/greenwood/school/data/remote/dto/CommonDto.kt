package com.greenwood.school.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The success envelope every non-binary backend endpoint wraps its payload in
 * (`com.school.sms.dto.response.ApiResponse`).
 *
 * `data` is nullable because the backend serialises with `@JsonInclude(NON_NULL)`
 * and its `ApiResponse.success(message)` overload omits the field entirely for
 * void operations (logout, delete, mark-alumni, …).
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = true,
    val message: String = "",
    val data: T? = null,
    val timestamp: String? = null,
)

/**
 * Spring page envelope (`com.school.sms.dto.response.PageResponse`).
 *
 * Field names follow the *backend*, not `school-frontend/src/types/index.ts` — the
 * frontend's `PageResponse` type declares `page`/`size`/`first`, which the API has
 * never actually sent. The real wire fields are the ones below.
 */
@Serializable
data class PageEnvelope<T>(
    val content: List<T> = emptyList(),
    val pageNumber: Int = 0,
    val pageSize: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val last: Boolean = true,
)

/**
 * Error body produced by `GlobalExceptionHandler`. Note this is *not* the success
 * envelope — there is no `success` field on failures, so it must be parsed separately.
 */
@Serializable
data class ErrorEnvelope(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    @SerialName("validationErrors")
    val validationErrors: Map<String, String>? = null,
)

/** `{ "photoUrl": "..." }`, `{ "promoted": 12 }` and friends — small ad-hoc maps. */
@Serializable
data class StringMapDto(val values: Map<String, String> = emptyMap())
