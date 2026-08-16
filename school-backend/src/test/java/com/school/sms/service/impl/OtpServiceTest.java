package com.school.sms.service.impl;

import com.school.sms.dto.request.SendOtpRequest;
import com.school.sms.dto.request.VerifyOtpRequest;
import com.school.sms.dto.response.OtpVerifyResponse;
import com.school.sms.entity.OtpChannel;
import com.school.sms.entity.OtpCode;
import com.school.sms.entity.OtpPurpose;
import com.school.sms.entity.PasswordResetToken;
import com.school.sms.entity.User;
import com.school.sms.exception.BadRequestException;
import com.school.sms.exception.TooManyRequestsException;
import com.school.sms.repository.OtpCodeRepository;
import com.school.sms.repository.PasswordResetTokenRepository;
import com.school.sms.repository.UserRepository;
import com.school.sms.service.EmailService;
import com.school.sms.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A six-digit code is only a million possibilities, so almost everything that
 * makes this safe is a rule about when a code stops working rather than about
 * the code itself. These tests are those rules.
 *
 * The recurring assertion is that failures are indistinguishable: an unknown
 * address, an expired code and a wrong code must all come back identical, or the
 * endpoint becomes a way to discover which accounts exist.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceTest {

    private static final String EMAIL = "parent@example.com";
    private static final String IP = "203.0.113.9";
    private static final String REJECTION = "That code is incorrect or has expired. Please request a new one.";

    @Mock private OtpCodeRepository otpCodeRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;
    @Mock private OtpRateLimiter rateLimiter;
    @Mock private com.school.sms.service.AuthService authService;
    @Mock private OtpAttemptRecorder attemptRecorder;

    @InjectMocks private OtpServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = userWith(7L, EMAIL, "Asha");
        when(rateLimiter.tryAcquire(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(otpCodeRepository.findFirstByUserOrderByIdDesc(any())).thenReturn(Optional.empty());
    }

    /* ---- sending ----------------------------------------------------------- */

    @Test
    void sending_to_an_unknown_address_looks_exactly_like_success() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        var response = service.send(request("nobody@example.com"), IP);

        // Same body a real account gets…
        assertThat(response.getExpiresInSeconds()).isEqualTo(300);
        // …and nothing sent or stored, so there is no timing or side effect to read either.
        verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), anyString());
        verify(otpCodeRepository, never()).save(any());
    }

    @Test
    void a_sent_code_is_six_digits_never_starting_with_zero_and_is_stored_hashed() {
        service.send(request(EMAIL), IP);

        var sentCode = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(EMAIL), eq("Asha"), sentCode.capture(), eq(5), anyString());

        assertThat(sentCode.getValue()).matches("^[1-9][0-9]{5}$");

        var saved = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(saved.capture());
        // The digits must not be recoverable from the row.
        assertThat(saved.getValue().getCodeHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getCodeHash()).isNotEqualTo(sentCode.getValue());
        assertThat(saved.getValue().getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);
        assertThat(saved.getValue().getChannel()).isEqualTo(OtpChannel.EMAIL);
    }

    @Test
    void requesting_a_new_code_retires_the_previous_one() {
        service.send(request(EMAIL), IP);

        // Two live codes would double the guesses that work.
        verify(otpCodeRepository).consumeOutstanding(eq(user), eq(OtpPurpose.PASSWORD_RESET), any());
    }

    @Test
    void a_second_code_within_the_cooldown_is_refused() {
        OtpCode justSent = OtpCode.builder().createdAt(LocalDateTime.now().minusSeconds(5)).build();
        when(otpCodeRepository.findFirstByUserOrderByIdDesc(user)).thenReturn(Optional.of(justSent));

        assertThatThrownBy(() -> service.send(request(EMAIL), IP))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void too_many_codes_in_an_hour_are_refused() {
        when(otpCodeRepository.countByUserAndCreatedAtAfter(eq(user), any())).thenReturn(5L);

        assertThatThrownBy(() -> service.send(request(EMAIL), IP))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void an_exhausted_ip_is_refused_before_the_account_is_even_looked_up() {
        when(rateLimiter.tryAcquire(eq(IP), org.mockito.ArgumentMatchers.anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.send(request(EMAIL), IP))
                .isInstanceOf(TooManyRequestsException.class);

        // Looking up first would make a throttled request measurably slower for a
        // real address than an unknown one.
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void one_code_goes_to_both_the_email_and_the_phone_on_the_account() {
        user.setPhone("+91-9810011122");
        when(smsService.isAvailable()).thenReturn(true);

        service.send(request(EMAIL), IP);

        var emailed = ArgumentCaptor.forClass(String.class);
        var texted = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(EMAIL), anyString(), emailed.capture(), org.mockito.ArgumentMatchers.anyInt(), anyString());
        verify(smsService).send(eq("+91-9810011122"), texted.capture());

        // One code, two messages — not two codes, which would mean whichever
        // arrived second silently invalidated the first.
        assertThat(texted.getValue()).contains(emailed.getValue());

        var saved = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(saved.capture());
        assertThat(saved.getValue().getChannel()).isEqualTo(OtpChannel.BOTH);
    }

    @Test
    void the_code_goes_to_the_accounts_own_details_not_to_whatever_was_typed() {
        user.setPhone("+91-9810011122");
        when(smsService.isAvailable()).thenReturn(true);
        when(userRepository.findAllByPhoneDigits("9810011122")).thenReturn(List.of(user));

        // Started from the phone number…
        service.send(request("9810011122"), IP);

        // …and the email still goes out, so someone who signs in with their number
        // does not have to remember which detail the code was sent to.
        verify(emailService).sendOtpEmail(eq(EMAIL), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), anyString());
        verify(smsService).send(eq("+91-9810011122"), anyString());
    }

    @Test
    void email_alone_still_works_when_there_is_no_gateway() {
        user.setPhone("+91-9810011122");
        when(smsService.isAvailable()).thenReturn(false);

        service.send(request(EMAIL), IP);

        // No longer refused: the other channel carries it. The whole request used
        // to fail when SMS was unavailable.
        verify(emailService).sendOtpEmail(eq(EMAIL), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), anyString());
        verify(smsService, never()).send(anyString(), anyString());

        var saved = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(saved.capture());
        assertThat(saved.getValue().getChannel()).isEqualTo(OtpChannel.EMAIL);
    }

    @Test
    void an_account_with_nowhere_to_reach_it_still_answers_like_every_other() {
        user.setEmail("");
        user.setPhone(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        var response = service.send(request(EMAIL), IP);

        // Saying "this account has no contact details" would confirm it exists.
        assertThat(response.getExpiresInSeconds()).isEqualTo(300);
        verify(otpCodeRepository, never()).save(any());
    }

    @Test
    void a_phone_number_on_more_than_one_account_is_treated_as_no_match() {
        User sibling = userWith(8L, "other@example.com", "Ravi");
        when(userRepository.findAllByPhoneDigits("9810011122")).thenReturn(List.of(user, sibling));

        service.send(request("9810011122"), IP);

        // Guessing which account was meant would send one person's code to another.
        verify(otpCodeRepository, never()).save(any());
    }

    /* ---- verifying --------------------------------------------------------- */

    @Test
    void a_correct_code_is_spent_and_returns_a_reset_token() {
        OtpCode live = liveCode();
        when(otpCodeRepository.findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(user, OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(live));
        when(passwordEncoder.matches("482915", "hashed")).thenReturn(true);

        OtpVerifyResponse response = service.verify(attempt("482915"));

        assertThat(response.getResetToken()).isNotBlank();
        // Single use: consuming it here is what stops the same digits working twice.
        assertThat(live.getConsumedAt()).isNotNull();
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void a_wrong_code_counts_against_the_attempt_limit() {
        OtpCode live = liveCode();
        live.setId(42L);
        when(otpCodeRepository.findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(user, OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(live));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.verify(attempt("000000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(REJECTION);

        // Through the recorder, not inline: the rejection rolls this transaction
        // back, so an increment saved here would be undone and the cap would never
        // bite. Asserting the delegation is what stops that regressing.
        verify(attemptRecorder).recordFailedAttempt(42L);
        verify(otpCodeRepository, never()).save(live);
    }

    @Test
    void a_correct_code_is_still_refused_once_the_attempts_are_gone() {
        OtpCode burned = liveCode();
        burned.setAttempts(5);
        when(otpCodeRepository.findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(user, OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(burned));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.verify(attempt("482915")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(REJECTION);
    }

    @Test
    void an_expired_code_is_refused() {
        OtpCode stale = liveCode();
        stale.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(otpCodeRepository.findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(user, OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(stale));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.verify(attempt("482915")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(REJECTION);
    }

    @Test
    void a_code_issued_for_one_purpose_cannot_be_used_for_another() {
        // The lookup is scoped by purpose, so a password-reset code simply is not
        // found when presented as a login code.
        when(otpCodeRepository.findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(user, OtpPurpose.LOGIN))
                .thenReturn(Optional.empty());

        VerifyOtpRequest login = new VerifyOtpRequest(EMAIL, OtpPurpose.LOGIN, "482915");

        assertThatThrownBy(() -> service.verify(login))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(REJECTION);
    }

    @Test
    void verifying_against_an_unknown_address_fails_identically_to_a_wrong_code() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(new VerifyOtpRequest("nobody@example.com", OtpPurpose.PASSWORD_RESET, "482915")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(REJECTION);
    }

    @Test
    void an_address_is_matched_regardless_of_case_or_spacing() {
        service.send(request("  " + EMAIL.toUpperCase() + " "), IP);

        verify(emailService).sendOtpEmail(eq(EMAIL), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), anyString());
    }

    /* ---- signing in with a code -------------------------------------------- */

    @Test
    void a_correct_login_code_returns_the_same_token_pair_a_password_would() {
        OtpCode live = liveCode();
        live.setPurpose(OtpPurpose.LOGIN);
        when(otpCodeRepository.findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(user, OtpPurpose.LOGIN))
                .thenReturn(Optional.of(live));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        var tokens = com.school.sms.dto.response.JwtAuthResponse.builder().accessToken("access").build();
        when(authService.loginWithVerifiedOtp(user)).thenReturn(tokens);

        OtpVerifyResponse response = service.verify(new VerifyOtpRequest(EMAIL, OtpPurpose.LOGIN, "482915"));

        // Reusing the ordinary response means refresh, logout and the clients'
        // session handling need no special case for a code-issued session.
        assertThat(response.getAuth()).isSameAs(tokens);
        assertThat(response.getResetToken()).isNull();
        assertThat(live.getConsumedAt()).isNotNull();
    }

    @Test
    void a_login_code_never_bypasses_the_deactivated_account_check() {
        OtpCode live = liveCode();
        live.setPurpose(OtpPurpose.LOGIN);
        when(otpCodeRepository.findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(user, OtpPurpose.LOGIN))
                .thenReturn(Optional.of(live));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        // Password login gets this refusal from Spring Security; the code path has
        // to ask for it, so this proves it does.
        when(authService.loginWithVerifiedOtp(user))
                .thenThrow(new org.springframework.security.authentication.DisabledException("Your account is not active."));

        assertThatThrownBy(() -> service.verify(new VerifyOtpRequest(EMAIL, OtpPurpose.LOGIN, "482915")))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class);
    }

    /* ---- fixtures ---------------------------------------------------------- */

    private OtpCode liveCode() {
        return OtpCode.builder()
                .user(user)
                .codeHash("hashed")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .channel(OtpChannel.EMAIL)
                .destination(EMAIL)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .build();
    }

    private SendOtpRequest request(String destination) {
        return new SendOtpRequest(destination, OtpPurpose.PASSWORD_RESET);
    }

    /** Named to stay out of the way of Mockito's static {@code verify}. */
    private VerifyOtpRequest attempt(String code) {
        return new VerifyOtpRequest(EMAIL, OtpPurpose.PASSWORD_RESET, code);
    }

    private User userWith(Long id, String email, String firstName) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFirstName(firstName);
        return u;
    }
}
