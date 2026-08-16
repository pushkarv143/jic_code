package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.ChangePasswordRequestDto
import com.greenwood.school.data.remote.dto.ForgotPasswordRequestDto
import com.greenwood.school.data.remote.dto.JwtAuthResponseDto
import com.greenwood.school.data.remote.dto.LoginRequestDto
import com.greenwood.school.data.remote.dto.OtpSendResponseDto
import com.greenwood.school.data.remote.dto.OtpVerifyResponseDto
import com.greenwood.school.data.remote.dto.RefreshTokenRequestDto
import com.greenwood.school.data.remote.dto.SendOtpRequestDto
import com.greenwood.school.data.remote.dto.VerifyOtpRequestDto
import com.greenwood.school.data.remote.dto.RegisterRequestDto
import com.greenwood.school.data.remote.dto.ResetPasswordRequestDto
import com.greenwood.school.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Endpoints under `/api/v1/auth`.
 *
 * Every path here is **relative** — the base URL already ends in `/api/v1/`, and a
 * leading slash would make Retrofit discard it. Same convention across all APIs.
 */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): ApiEnvelope<JwtAuthResponseDto>

    @POST("auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): ApiEnvelope<JwtAuthResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequestDto): ApiEnvelope<Unit>

    /** Self-registration is limited to STUDENT/PARENT and lands in a pending state. */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): ApiEnvelope<Unit>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto): ApiEnvelope<Unit>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): ApiEnvelope<Unit>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): ApiEnvelope<Unit>

    /**
     * Sends a one-time passcode. Succeeds whether or not the destination is
     * registered, so the response must not be read as confirmation that it is.
     */
    @POST("auth/otp/request")
    suspend fun requestOtp(@Body request: SendOtpRequestDto): ApiEnvelope<OtpSendResponseDto>

    /** Exchanges a correct code for a single-use reset token. */
    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequestDto): ApiEnvelope<OtpVerifyResponseDto>

    @GET("auth/me")
    suspend fun me(): ApiEnvelope<UserDto>
}
