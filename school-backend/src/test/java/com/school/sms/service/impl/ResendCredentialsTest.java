package com.school.sms.service.impl;

import com.school.sms.entity.RefreshToken;
import com.school.sms.entity.Student;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.repository.RefreshTokenRepository;
import com.school.sms.repository.StudentRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.service.AuditLogService;
import com.school.sms.service.EmailService;
import com.school.sms.service.SmsService;
import com.school.sms.util.CredentialGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Resending a student's credentials.
 *
 * <p>A credential reset performed on somebody else's account, so the things worth
 * pinning are the ones that would go unnoticed: that the username survives, that
 * the forced first-login reset is re-armed, that existing sessions die with the old
 * password, and that it is recorded.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResendCredentialsTest {

    private static final long STUDENT_ID = 507L;

    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;
    @Mock private AuditLogService auditLogService;
    @Mock private CredentialGenerator credentialGenerator;
    /**
     * findEntity() runs every read through this, so a resend is also subject to the
     * per-student scope check — an administrator can only reset a student they are
     * entitled to see in the first place.
     */
    @Mock private com.school.sms.security.StudentAccessGuard studentAccessGuard;

    @InjectMocks private StudentServiceImpl service;

    private User user;
    private Student student;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("pushkarv")
                .email("pushkar@example.com")
                .password("$2a$old")
                .firstName("Pushkar")
                .phone("9876543210")
                .mustChangePassword(false)
                .build();
        user.setId(596L);

        student = Student.builder().user(user).phone("9876543210").build();
        student.setId(STUDENT_ID);

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(credentialGenerator.temporaryPassword()).thenReturn("Kt7#mNqR4wYz");
        when(passwordEncoder.encode("Kt7#mNqR4wYz")).thenReturn("$2a$new");
        when(smsService.isAvailable()).thenReturn(true);
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(user)).thenReturn(List.of());
    }

    @Test
    @DisplayName("the username survives; only the password changes")
    void usernameIsUntouched() {
        service.resendCredentials(STUDENT_ID);

        // The username is the student's identity — on their timetable, in their
        // parents' notes — and a lost password says nothing about it. Regenerating
        // would also collide with this row and come back as "pushkarv1".
        assertThat(user.getUsername()).isEqualTo("pushkarv");
        assertThat(user.getPassword()).isEqualTo("$2a$new");
    }

    @Test
    @DisplayName("the forced first-login reset is re-armed")
    void forcedResetIsReArmed() {
        service.resendCredentials(STUDENT_ID);

        // The account is back on a password the student did not choose, so it may do
        // nothing but replace it — see PasswordChangeRequiredFilter.
        assertThat(user.isMustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("every live session is revoked, so the old password stops working everywhere")
    void sessionsAreRevoked() {
        RefreshToken live = RefreshToken.builder().user(user).token("tok").revoked(false).build();
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(user)).thenReturn(List.of(live));

        service.resendCredentials(STUDENT_ID);

        // Without this a device already signed in keeps working indefinitely on
        // credentials that no longer exist, and the forced reset applies to everyone
        // except the person holding that device — the one it was aimed at.
        assertThat(live.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(any());
    }

    @Test
    @DisplayName("the new password goes out by email and SMS")
    void deliveredBothWays() {
        service.resendCredentials(STUDENT_ID);

        verify(emailService).sendAccountCredentialsEmail(
                eq("pushkar@example.com"), eq("Pushkar"), eq("pushkarv"), eq("Kt7#mNqR4wYz"));

        ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
        verify(smsService).send(eq("9876543210"), sms.capture());
        assertThat(sms.getValue()).contains("pushkarv").contains("Kt7#mNqR4wYz");
    }

    @Test
    @DisplayName("recorded against the student, with the username and how many sessions went")
    void audited() {
        service.resendCredentials(STUDENT_ID);

        // A credential reset is done *to* somebody else's account, so who and when is
        // the point. The password itself is never written down.
        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).record(
                eq("RESEND_STUDENT_CREDENTIALS"), eq("Student"), eq(STUDENT_ID),
                any(), detail.capture());
        assertThat(detail.getValue()).contains("pushkarv").doesNotContain("Kt7#mNqR4wYz");
    }

    @Test
    @DisplayName("a missing SMS gateway leaves the email as the delivery rather than failing")
    void smsIsBestEffort() {
        when(smsService.isAvailable()).thenReturn(false);

        service.resendCredentials(STUDENT_ID);

        verify(emailService).sendAccountCredentialsEmail(anyString(), anyString(), anyString(), anyString());
        verify(smsService, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("a student with no login is refused rather than quietly given one")
    void refusesAStudentWithNoAccount() {
        student.setUser(null);

        assertThatThrownBy(() -> service.resendCredentials(STUDENT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no login");

        // Creating an account here would hide from the caller that this student was
        // admitted before provisioning was automatic.
        verify(emailService, never())
                .sendAccountCredentialsEmail(anyString(), anyString(), anyString(), anyString());
        verify(auditLogService, never()).record(anyString(), anyString(), anyLong(), any(), any());
    }
}
