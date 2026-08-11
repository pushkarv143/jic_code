package com.school.sms.service;

import com.school.sms.dto.request.ChangePasswordRequest;
import com.school.sms.dto.request.ForgotPasswordRequest;
import com.school.sms.dto.request.LoginRequest;
import com.school.sms.dto.request.RegisterRequest;
import com.school.sms.dto.request.ResetPasswordRequest;
import com.school.sms.dto.response.JwtAuthResponse;
import com.school.sms.dto.response.UserDto;

public interface AuthService {

    void register(RegisterRequest request);

    JwtAuthResponse login(LoginRequest request);

    JwtAuthResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserDto getCurrentUser(Long userId);
}
