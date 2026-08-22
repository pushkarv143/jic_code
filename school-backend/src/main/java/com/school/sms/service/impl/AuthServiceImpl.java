package com.school.sms.service.impl;

import com.school.sms.dto.request.ChangePasswordRequest;
import com.school.sms.dto.request.ForgotPasswordRequest;
import com.school.sms.dto.request.LoginRequest;
import com.school.sms.dto.request.ResetPasswordRequest;
import com.school.sms.dto.response.JwtAuthResponse;
import com.school.sms.dto.response.UserDto;
import com.school.sms.entity.PasswordResetToken;
import com.school.sms.entity.RefreshToken;
import com.school.sms.entity.Role;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.DuplicateResourceException;
import com.school.sms.exception.ResourceNotFoundException;
import com.school.sms.exception.TokenRefreshException;
import com.school.sms.exception.UnauthorizedException;
import com.school.sms.mapper.UserMapper;
import com.school.sms.entity.Permission;
import com.school.sms.repository.PasswordResetTokenRepository;
import com.school.sms.repository.PermissionRepository;
import com.school.sms.repository.RefreshTokenRepository;
import com.school.sms.repository.RoleRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.TeacherRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.security.JwtTokenProvider;
import com.school.sms.security.UserPrincipal;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.AuthService;
import com.school.sms.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AuditLogService auditLogService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final long PASSWORD_RESET_EXPIRY_MINUTES = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public JwtAuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.record("LOGIN", "User", user.getId(), null, null);

        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public JwtAuthResponse refreshToken(String requestRefreshToken) {
        if (!tokenProvider.validateToken(requestRefreshToken)) {
            throw new TokenRefreshException(requestRefreshToken, "Refresh token is invalid or expired");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token not found"));

        if (storedToken.isRevoked()) {
            throw new TokenRefreshException(requestRefreshToken, "Refresh token has been revoked");
        }
        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenRefreshException(requestRefreshToken, "Refresh token has expired");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return issueTokenPair(storedToken.getUser());
    }

    @Override
    @Transactional
    public void logout(String requestRefreshToken) {
        refreshTokenRepository.findByToken(requestRefreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = generateSecureToken();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiryDate(LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRY_MINUTES))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetLink);
        });
        // Always behave the same way regardless of whether the email exists, to avoid account enumeration.
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or unknown password reset token"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("This password reset token has already been used");
        }
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        revokeAllRefreshTokens(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            // Blocked because the generated password is the one that was emailed, and
            // "change it" has to mean changed. Without this, a first-time user could
            // satisfy the forced reset by re-entering what they were sent.
            throw new BadRequestException("The new password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // The account is now on a password only its owner knows, so the forced-reset
        // gate lifts. Cleared unconditionally rather than only when it was set: this
        // is the one place a password becomes self-chosen.
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Already revoked every refresh token before this change, which is what makes
        // the first-login flow end at the sign-in screen: the tokens issued against
        // the generated password stop working the moment it is replaced.
        revokeAllRefreshTokens(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return buildUserDto(user);
    }

    /**
     * Builds the login/refresh/me response DTO, enriching it with the caller's own
     * studentId/teacherId (and, for students, their current classId/sectionId) so the
     * frontend can resolve "which student/teacher am I" without a separate lookup call,
     * plus the role's permission names so the clients can render role-appropriate menus.
     */
    private UserDto buildUserDto(User user) {
        UserDto dto = userMapper.toDto(user);

        dto.setPermissions(permissionRepository.findAllByRoleId(user.getRole().getId()).stream()
                .map(Permission::getName)
                .toList());

        studentRepository.findByUserId(user.getId()).ifPresent(student -> {
            dto.setStudentId(student.getId());
            dto.setClassId(student.getSchoolClass() != null ? student.getSchoolClass().getId() : null);
            dto.setSectionId(student.getSection() != null ? student.getSection().getId() : null);
        });
        teacherRepository.findByUserId(user.getId()).ifPresent(teacher -> dto.setTeacherId(teacher.getId()));

        return dto;
    }

    @Override
    @Transactional
    public JwtAuthResponse loginWithVerifiedOtp(User user) {
        // The AuthenticationManager is what refuses a disabled account on the
        // password path, and it is not involved here at all. Without this a
        // deactivated user could still sign in with a code.
        if (!user.isActive()) {
            throw new DisabledException("Your account is not active. Please contact the school administrator.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        // Recorded distinctly from a password login so the audit trail shows how
        // the session was obtained.
        auditLogService.record("LOGIN_OTP", "User", user.getId(), null, null);

        return issueTokenPair(user);
    }

    private JwtAuthResponse issueTokenPair(User user) {
        UserPrincipal principal = UserPrincipal.create(user);

        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshTokenValue = tokenProvider.generateRefreshToken(principal);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return JwtAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(buildUserDto(user))
                // Tokens are issued even when this is true — the change-password
                // endpoint needs authenticating like any other. What the flag does is
                // tell the client to go straight there, and PasswordChangeRequiredFilter
                // makes sure nothing else works until it has.
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    private void revokeAllRefreshTokens(User user) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        activeTokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
