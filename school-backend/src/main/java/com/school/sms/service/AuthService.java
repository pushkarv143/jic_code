package com.school.sms.service;

import com.school.sms.dto.request.ChangePasswordRequest;
import com.school.sms.dto.request.ForgotPasswordRequest;
import com.school.sms.dto.request.LoginRequest;
import com.school.sms.dto.request.RegisterRequest;
import com.school.sms.dto.request.ResetPasswordRequest;
import com.school.sms.dto.response.JwtAuthResponse;
import com.school.sms.dto.response.UserDto;
import com.school.sms.entity.User;

public interface AuthService {

    void register(RegisterRequest request);

    JwtAuthResponse login(LoginRequest request);

    /**
     * Issues a session to a user who has already proved they own the account by
     * answering a one-time passcode.
     *
     * Separate from {@link #login} because that path goes through Spring
     * Security's AuthenticationManager, which is what normally rejects a disabled
     * account. Nothing in the passcode flow touches that, so this repeats the
     * check explicitly — otherwise a deactivated user could sign in with a code
     * after their password had stopped working.
     *
     * Callers must have verified a code for {@code OtpPurpose.LOGIN} first; this
     * method takes that as given and does no verification of its own.
     */
    JwtAuthResponse loginWithVerifiedOtp(User user);

    JwtAuthResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserDto getCurrentUser(Long userId);
}
