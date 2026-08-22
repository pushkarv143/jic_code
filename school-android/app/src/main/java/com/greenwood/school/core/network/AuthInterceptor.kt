package com.greenwood.school.core.network

import com.greenwood.school.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches `Authorization: Bearer <accessToken>` to every request except the
 * unauthenticated auth endpoints.
 *
 * Mirrors the web client's axios request interceptor.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.isPublicEndpoint()) return chain.proceed(request)
        // Something upstream already set it (the authenticator's replay) — leave it be.
        if (request.header(HEADER_AUTHORIZATION) != null) return chain.proceed(request)

        val token = sessionManager.accessTokenBlocking() ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $token")
                .build(),
        )
    }

    private fun okhttp3.Request.isPublicEndpoint(): Boolean {
        val path = url.encodedPath
        return PUBLIC_PATHS.any { path.endsWith(it) }
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"

        /**
         * Endpoints the backend's SecurityConfig permits without a token. Sending a
         * stale Bearer to these would be harmless but noisy, and `/auth/refresh-token`
         * in particular must never carry an expired access token.
         */
        private val PUBLIC_PATHS = listOf(
            "/auth/login",
            "/auth/refresh-token",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/public/admission-enquiries",
        )
    }
}
