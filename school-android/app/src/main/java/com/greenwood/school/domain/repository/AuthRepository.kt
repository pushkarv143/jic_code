package com.greenwood.school.domain.repository

import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.data.remote.dto.OtpSendResponseDto
import com.greenwood.school.data.remote.dto.OtpVerifyResponseDto
import com.greenwood.school.data.remote.dto.UserDto
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns sign-in, sign-out and "who am I". The only repository that mutates
 * [com.greenwood.school.core.session.SessionManager].
 */
interface AuthRepository {

    /** Emits the signed-in user, or null when signed out. Drives the nav graph's auth gate. */
    val currentUser: StateFlow<UserDto?>

    suspend fun login(username: String, password: String): ApiResult<UserDto>

    /**
     * Revokes the refresh token server-side, then clears local state.
     *
     * Local state is cleared even when the network call fails — a user who taps
     * "Sign out" must end up signed out on this device regardless.
     */
    suspend fun logout(): ApiResult<Unit>

    suspend fun forgotPassword(email: String): ApiResult<Unit>

    suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit>

    /**
     * Asks for a passcode at [destination] — an email address or a phone number.
     *
     * Success does not mean the address is registered; the server answers the same
     * way either way, so the screen must not imply otherwise.
     */
    suspend fun requestOtp(destination: String, purpose: String): ApiResult<OtpSendResponseDto>

    /** Returns a single-use reset token when [code] is right. */
    suspend fun verifyOtp(destination: String, purpose: String, code: String): ApiResult<OtpVerifyResponseDto>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
    ): ApiResult<Unit>

    /** Refreshes the cached profile from `/auth/me`. */
    suspend fun refreshProfile(): ApiResult<UserDto>
}
