package com.school.sms.controller;

import com.school.sms.dto.request.ChangePasswordRequest;
import com.school.sms.dto.request.ForgotPasswordRequest;
import com.school.sms.dto.request.LoginRequest;
import com.school.sms.dto.request.RefreshTokenRequest;
import com.school.sms.dto.request.RegisterRequest;
import com.school.sms.dto.request.ResetPasswordRequest;
import com.school.sms.dto.request.SendOtpRequest;
import com.school.sms.dto.request.VerifyOtpRequest;
import com.school.sms.dto.response.ApiResponse;
import com.school.sms.dto.response.JwtAuthResponse;
import com.school.sms.dto.response.OtpSendResponse;
import com.school.sms.dto.response.OtpVerifyResponse;
import com.school.sms.dto.response.UserDto;
import com.school.sms.security.SecurityUtils;
import com.school.sms.service.AuthService;
import com.school.sms.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token refresh, logout and password management")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    @Operation(summary = "Self-register a STUDENT or PARENT account, pending admin approval")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Registration submitted. You can sign in once your account is approved by the school."));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and issue a JWT access/refresh token pair")
    public ResponseEntity<ApiResponse<JwtAuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtAuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Exchange a valid refresh token for a new access/refresh token pair")
    public ResponseEntity<ApiResponse<JwtAuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        JwtAuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token, logging the user out of that session")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link by email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for that email, a password reset link has been sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a valid password-reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully"));
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Send a one-time passcode to the email or phone on an account. "
            + "Always reports success, whether or not the destination is registered.")
    public ResponseEntity<ApiResponse<OtpSendResponse>> requestOtp(@Valid @RequestBody SendOtpRequest request,
                                                                    HttpServletRequest http) {
        OtpSendResponse result = otpService.send(request, clientIpOf(http));
        return ResponseEntity.ok(ApiResponse.success(
                "If an account matches, a code has been sent.", result));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Exchange a correct passcode for a single-use password-reset token")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Code verified", otpService.verify(request)));
    }

    /**
     * Caddy sits in front of the app, so the socket address is the proxy's. Prefer
     * the forwarded header and take its first entry, which is the original client;
     * later entries are proxies and are trivially spoofed by the caller.
     */
    private String clientIpOf(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change the current authenticated user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        UserDto userDto = authService.getCurrentUser(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved", userDto));
    }
}
