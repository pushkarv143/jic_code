package com.school.sms.service.impl;

import com.school.sms.entity.User;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.RefreshTokenRepository;
import com.school.sms.repository.PasswordResetTokenRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.mapper.UserMapper;
import com.school.sms.security.JwtTokenProvider;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Signing in with a passcode skips the AuthenticationManager entirely, and the
 * AuthenticationManager is what normally refuses a deactivated account. So the
 * check has to be made by hand on that path, and this is the test that it is.
 *
 * Only the rejection is exercised here: it returns before any token is minted, so
 * the test needs none of the token machinery and stays honest about what it covers.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpLoginAccountStateTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private UserMapper userMapper;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;

    @InjectMocks private AuthServiceImpl authService;

    @Test
    void a_deactivated_account_cannot_sign_in_with_a_passcode() {
        User disabled = new User();
        disabled.setId(7L);
        disabled.setActive(false);

        assertThatThrownBy(() -> authService.loginWithVerifiedOtp(disabled))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("not active");

        // No session, and no last-login stamp implying one happened.
        verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
