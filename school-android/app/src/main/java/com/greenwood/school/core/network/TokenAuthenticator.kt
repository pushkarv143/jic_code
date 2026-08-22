package com.greenwood.school.core.network

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.JwtAuthResponseDto
import com.greenwood.school.data.remote.dto.RefreshTokenRequestDto
import com.greenwood.school.core.session.AuthEvent
import com.greenwood.school.core.session.AuthEventBus
import com.greenwood.school.core.session.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Refresh-on-401, the Android counterpart of the axios response interceptor in
 * `school-frontend/src/api/axiosInstance.ts`.
 *
 * OkHttp calls this only when a request comes back 401, and it automatically
 * gives up after a handful of retries of the same call, so there is no risk of
 * the infinite loop the web client guards against with its `_retry` flag.
 *
 * Concurrency: several requests can 401 at once. The `synchronized` block plus the
 * "has the token already changed?" check means exactly one refresh call goes out
 * and every other caller picks up its result — equivalent to the web client's
 * `isRefreshing` + `pendingQueue`.
 *
 * The refresh call itself is made on a *bare* OkHttp client with no interceptors,
 * so it can never recurse back into this authenticator.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val authEventBus: AuthEventBus,
    private val json: Json,
    @BaseUrl private val baseUrl: String,
    @RefreshClient private val refreshClient: Provider<OkHttpClient>,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Give up rather than loop: OkHttp re-invokes us for each retry of the same call.
        if (response.priorResponseCount() >= MAX_RETRIES) return null

        val failedToken = response.request.header(AuthInterceptor.HEADER_AUTHORIZATION)
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(lock) {
            val currentToken = sessionManager.accessTokenBlocking()

            // Another thread refreshed while we waited on the lock — just replay.
            if (currentToken != null && currentToken != failedToken) {
                return response.request.withToken(currentToken)
            }

            val refreshToken = sessionManager.refreshTokenBlocking()
            if (refreshToken.isNullOrBlank()) {
                signOut()
                return null
            }

            val refreshed = refresh(refreshToken)
            if (refreshed == null) {
                signOut()
                return null
            }

            sessionManager.saveRefreshedBlocking(refreshed)
            // Tells AccessStore to re-read the caller's grants. This authenticator is
            // synchronous and cannot fetch, so the work is handed to a collector.
            authEventBus.publish(AuthEvent.TokensRefreshed)
            return response.request.withToken(refreshed.accessToken)
        }
    }

    private fun refresh(refreshToken: String): JwtAuthResponseDto? = runCatching {
        val body = json.encodeToString(RefreshTokenRequestDto.serializer(), RefreshTokenRequestDto(refreshToken))
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + REFRESH_PATH)
            .post(body)
            .build()

        refreshClient.get().newCall(request).execute().use { httpResponse ->
            if (!httpResponse.isSuccessful) return null
            val payload = httpResponse.body?.string() ?: return null
            json.decodeFromString(
                ApiEnvelope.serializer(JwtAuthResponseDto.serializer()),
                payload,
            ).data
        }
    }.getOrNull()

    private fun signOut() {
        sessionManager.clearBlocking()
        authEventBus.publish(AuthEvent.SessionExpired)
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(AuthInterceptor.HEADER_AUTHORIZATION, "Bearer $token").build()

    private fun Response.priorResponseCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRIES = 1
        const val REFRESH_PATH = "/auth/refresh-token"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
