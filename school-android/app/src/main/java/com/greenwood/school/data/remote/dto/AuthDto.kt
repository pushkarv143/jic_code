package com.greenwood.school.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `com.school.sms.dto.response.UserDto`.
 *
 * `studentId` / `teacherId` / `classId` / `sectionId` are populated **only** on
 * `/auth/login`, `/auth/refresh-token` and `/auth/me` — never on the admin
 * `/users` endpoints. They answer "which student/teacher am I", which the
 * self-service screens need before they can call anything scoped to a person.
 */
@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val role: String,
    val active: Boolean = true,
    val profileImage: String? = null,
    val lastLogin: String? = null,
    val createdAt: String? = null,
    val studentId: Long? = null,
    val teacherId: Long? = null,
    val classId: Long? = null,
    val sectionId: Long? = null,
    /**
     * Permission names granted to this user's role (e.g. `STUDENT_VIEW`), returned by
     * `/auth/login`, `/auth/refresh-token` and `/auth/me`. Defaults to empty so a
     * session persisted before this field existed still deserializes; see
     * [com.greenwood.school.core.session.UserSession.permissions] for how an empty
     * set is interpreted.
     */
    val permissions: List<String> = emptyList(),
) {
    val fullName: String
        get() = listOfNotNull(firstName?.takeIf { it.isNotBlank() }, lastName?.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .ifBlank { username }

    val initials: String
        get() = fullName.split(' ')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { username.take(2).uppercase() }
}

/** `com.school.sms.dto.response.JwtAuthResponse`. `expiresIn` is in **seconds** (900). */
@Serializable
data class JwtAuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 0,
    val user: UserDto,
    /**
     * True when this account is still on the password the school generated for it.
     *
     * <p>Tokens are issued anyway — changing a password needs authenticating like
     * anything else — but every other endpoint answers 403 until it is replaced.
     * Defaulted to false so an older server that does not send the field is read as
     * "nothing to do", which is what it means.
     */
    val mustChangePassword: Boolean = false,
)

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class RefreshTokenRequestDto(val refreshToken: String)

@Serializable
data class ForgotPasswordRequestDto(val email: String)

@Serializable
data class ResetPasswordRequestDto(val token: String, val newPassword: String)

/* ---- One-time passcodes ----------------------------------------------------- */

/**
 * `destination` is an email address or a phone number in one field — the server
 * decides which it is, so the screen does not have to ask.
 *
 * `purpose` is always sent explicitly: the API refuses to default it, so a client
 * cannot obtain a login code by leaving a field out.
 */
@Serializable
data class SendOtpRequestDto(val destination: String, val purpose: String)

@Serializable
data class VerifyOtpRequestDto(val destination: String, val purpose: String, val code: String)

/**
 * Says nothing about whether the account exists — that is deliberate on the
 * server side. Only the two timings the screen needs to run its countdown.
 */
@Serializable
data class OtpSendResponseDto(
    val expiresInSeconds: Int = 300,
    val resendAfterSeconds: Int = 60,
)

/** For `PASSWORD_RESET` the reset token is set; `auth` is reserved for OTP login. */
@Serializable
data class OtpVerifyResponseDto(
    val resetToken: String? = null,
    val auth: JwtAuthResponseDto? = null,
)

/** Mirrors the backend's OtpPurpose enum; sent as its name. */
object OtpPurpose {
    const val PASSWORD_RESET = "PASSWORD_RESET"
    const val LOGIN = "LOGIN"
    const val PHONE_VERIFY = "PHONE_VERIFY"
}

@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String,
    /** The backend validates this alongside `newPassword` and rejects the request without it. */
    val confirmPassword: String,
)
