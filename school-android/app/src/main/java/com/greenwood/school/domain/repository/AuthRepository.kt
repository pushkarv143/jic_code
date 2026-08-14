package com.greenwood.school.domain.repository

import com.greenwood.school.core.network.ApiResult
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

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        username: String,
        password: String,
        role: String,
    ): ApiResult<Unit>

    suspend fun forgotPassword(email: String): ApiResult<Unit>

    suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit>

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit>

    /** Refreshes the cached profile from `/auth/me`. */
    suspend fun refreshProfile(): ApiResult<UserDto>
}
