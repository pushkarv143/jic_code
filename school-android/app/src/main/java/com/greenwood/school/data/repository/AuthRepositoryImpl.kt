package com.greenwood.school.data.repository

import com.greenwood.school.core.common.IoDispatcher
import com.greenwood.school.core.network.ApiResult
import com.greenwood.school.core.network.ErrorMapper
import com.greenwood.school.core.network.map
import com.greenwood.school.core.network.onSuccess
import com.greenwood.school.core.session.SessionManager
import com.greenwood.school.data.remote.api.AuthApi
import com.greenwood.school.data.remote.dto.ChangePasswordRequestDto
import com.greenwood.school.data.remote.dto.ForgotPasswordRequestDto
import com.greenwood.school.data.remote.dto.LoginRequestDto
import com.greenwood.school.data.remote.dto.RefreshTokenRequestDto
import com.greenwood.school.data.remote.dto.RegisterRequestDto
import com.greenwood.school.data.remote.dto.ResetPasswordRequestDto
import com.greenwood.school.data.remote.dto.UserDto
import com.greenwood.school.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.greenwood.school.core.common.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager,
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationScope applicationScope: CoroutineScope,
    errorMapper: ErrorMapper,
) : BaseRepository(dispatcher, errorMapper), AuthRepository {

    override val currentUser: StateFlow<UserDto?> = sessionManager.session
        .map { it?.user }
        .stateIn(applicationScope, SharingStarted.Eagerly, sessionManager.currentUser)

    override suspend fun login(username: String, password: String): ApiResult<UserDto> =
        call { api.login(LoginRequestDto(username.trim(), password)) }
            .onSuccess { sessionManager.save(it) }
            .map { it.user }

    override suspend fun logout(): ApiResult<Unit> {
        val refreshToken = sessionManager.session.value?.refreshToken
        val result = if (refreshToken.isNullOrBlank()) {
            ApiResult.Success(Unit)
        } else {
            ack { api.logout(RefreshTokenRequestDto(refreshToken)) }
        }
        // Always clear locally: a failed revoke must not strand the user signed in.
        sessionManager.clear()
        return result
    }

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        username: String,
        password: String,
        role: String,
    ): ApiResult<Unit> = ack {
        api.register(
            RegisterRequestDto(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                email = email.trim(),
                phone = phone.trim(),
                username = username.trim(),
                password = password,
                role = role,
            ),
        )
    }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> =
        ack { api.forgotPassword(ForgotPasswordRequestDto(email.trim())) }

    override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> =
        ack { api.resetPassword(ResetPasswordRequestDto(token, newPassword)) }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
    ): ApiResult<Unit> =
        ack { api.changePassword(ChangePasswordRequestDto(currentPassword, newPassword, confirmPassword)) }

    override suspend fun refreshProfile(): ApiResult<UserDto> =
        call { api.me() }.onSuccess { sessionManager.updateUser(it) }
}
