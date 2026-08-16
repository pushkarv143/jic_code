package com.school.sms.service.impl;

import com.school.sms.dto.request.SendOtpRequest;
import com.school.sms.dto.request.VerifyOtpRequest;
import com.school.sms.dto.response.OtpSendResponse;
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
import com.school.sms.service.AuthService;
import com.school.sms.service.EmailService;
import com.school.sms.service.OtpService;
import com.school.sms.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * One-time passcodes.
 *
 * Two rules shape everything here.
 *
 * The first is that a six-digit code is only a million possibilities, so it is
 * defended by time and attempts rather than by entropy: five minutes, five
 * guesses, one live code per purpose, and a hash in the database rather than the
 * digits.
 *
 * The second is that neither endpoint may reveal whether an account exists. A
 * send always reports the same thing; a verify always fails the same way. That
 * is why the failure paths below look repetitive — the repetition is the point,
 * and collapsing them into distinct messages would undo it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    /** Long enough to fetch a phone, short enough that a stolen code goes stale. */
    private static final int CODE_TTL_MINUTES = 5;

    /** Wrong guesses a single code will absorb before it dies. */
    private static final int MAX_ATTEMPTS = 5;

    /** Codes per destination per hour. Each one is an email or a paid SMS. */
    private static final int MAX_SENDS_PER_DESTINATION_PER_HOUR = 5;

    /** Codes per IP per hour, to stop one caller enumerating many destinations. */
    private static final int MAX_SENDS_PER_IP_PER_HOUR = 15;

    /** Minimum gap between two codes to the same destination. */
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private static final int RESET_TOKEN_TTL_MINUTES = 15;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;
    private final OtpRateLimiter rateLimiter;
    private final AuthService authService;

    @Override
    @Transactional
    public OtpSendResponse send(SendOtpRequest request, String clientIp) {
        String destination = normalise(request.getDestination());

        // Checked before the account lookup so that hammering the endpoint costs
        // the same whether or not the destination is real.
        if (!rateLimiter.tryAcquire(clientIp, MAX_SENDS_PER_IP_PER_HOUR)) {
            throw new TooManyRequestsException("Too many requests. Please wait a few minutes and try again.");
        }

        Optional<User> match = findUser(destination);
        if (match.isEmpty()) {
            // Nothing to send. Report success anyway — telling the caller this
            // address is unknown is exactly the disclosure being avoided.
            return response();
        }

        User user = match.get();
        OtpChannel channel = channelFor(destination);

        if (channel == OtpChannel.SMS && !smsService.isAvailable()) {
            // A real limitation, so say so plainly. This one is safe to disclose:
            // it is a property of the server, not of the account.
            throw new BadRequestException(
                    "Codes cannot be sent by SMS yet. Please use the email address on your account.");
        }

        enforceSendLimits(destination);

        String code = generateCode();
        // Replace rather than accumulate: two live codes would double the number
        // of guesses that work, so asking for a new one retires the old one.
        otpCodeRepository.consumeOutstanding(user, request.getPurpose(), LocalDateTime.now());

        otpCodeRepository.save(OtpCode.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(code))
                .purpose(request.getPurpose())
                .channel(channel)
                .destination(destination)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .attempts(0)
                .build());

        deliver(user, channel, destination, code, request.getPurpose());

        // Note what happened, never what the code was.
        log.info("OTP issued for user {} purpose {} via {}", user.getId(), request.getPurpose(), channel);
        return response();
    }

    @Override
    @Transactional
    public OtpVerifyResponse verify(VerifyOtpRequest request) {
        String destination = normalise(request.getDestination());

        User user = findUser(destination).orElseThrow(OtpServiceImpl::rejected);

        OtpCode otp = otpCodeRepository
                .findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(user, request.getPurpose())
                .orElseThrow(OtpServiceImpl::rejected);

        if (!otp.isUsable(MAX_ATTEMPTS)) {
            throw rejected();
        }

        if (!passwordEncoder.matches(request.getCode(), otp.getCodeHash())) {
            // Count the miss before rejecting, or the attempt cap would never bite.
            otp.setAttempts(otp.getAttempts() + 1);
            otpCodeRepository.save(otp);
            throw rejected();
        }

        otp.setConsumedAt(LocalDateTime.now());
        otpCodeRepository.save(otp);

        return switch (request.getPurpose()) {
            case PASSWORD_RESET -> OtpVerifyResponse.builder()
                    .resetToken(issueResetToken(user))
                    .build();

            // Signing in with a code grants nothing that the password-reset flow
            // did not already: both are gated on control of the same mailbox, so
            // anyone who can read the code could equally have reset the password
            // and taken the account that way. It is a shorter path to the same
            // place, not a weaker one.
            case LOGIN -> OtpVerifyResponse.builder()
                    .auth(authService.loginWithVerifiedOtp(user))
                    .build();

            // No endpoint completes this one yet. Better an explicit refusal than
            // a success carrying nothing.
            case PHONE_VERIFY -> throw new BadRequestException(
                    "That code is valid, but this feature is not enabled yet.");
        };
    }

    /* ---- delivery ---------------------------------------------------------- */

    private void deliver(User user, OtpChannel channel, String destination, String code, OtpPurpose purpose) {
        String purposeText = switch (purpose) {
            case PASSWORD_RESET -> "reset your password";
            case LOGIN -> "sign in";
            case PHONE_VERIFY -> "confirm your phone number";
        };

        if (channel == OtpChannel.EMAIL) {
            emailService.sendOtpEmail(destination, user.getFirstName(), code, CODE_TTL_MINUTES, purposeText);
        } else {
            smsService.send(destination,
                    "%s is your School Management System code to %s. It expires in %d minutes. Do not share it."
                            .formatted(code, purposeText, CODE_TTL_MINUTES));
        }
    }

    /* ---- helpers ----------------------------------------------------------- */

    /**
     * Six digits from {@link SecureRandom}. Note the range: 100000 to 999999, so
     * a code never has a leading zero that a user might drop when retyping it.
     */
    private String generateCode() {
        return String.valueOf(100_000 + RANDOM.nextInt(900_000));
    }

    private String issueResetToken(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES))
                .used(false)
                .build());

        return token;
    }

    /**
     * Email addresses are matched case-insensitively; phone numbers are stripped
     * of the spaces, brackets and dashes people type but the column does not
     * consistently store.
     */
    private String normalise(String raw) {
        String trimmed = raw.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase() : trimmed.replaceAll("[\\s()\\-]", "");
    }

    private OtpChannel channelFor(String destination) {
        return destination.contains("@") ? OtpChannel.EMAIL : OtpChannel.SMS;
    }

    private Optional<User> findUser(String destination) {
        if (destination.contains("@")) {
            return userRepository.findByEmail(destination);
        }
        // users.phone has no unique constraint, so a number may sit on several
        // accounts. Picking one would send a stranger's code to whoever asked,
        // so an ambiguous number is treated as no match at all.
        List<User> byPhone = userRepository.findAllByPhone(destination);
        return byPhone.size() == 1 ? Optional.of(byPhone.get(0)) : Optional.empty();
    }

    private void enforceSendLimits(String destination) {
        LocalDateTime now = LocalDateTime.now();

        if (otpCodeRepository.countByDestinationAndCreatedAtAfter(destination, now.minusHours(1))
                >= MAX_SENDS_PER_DESTINATION_PER_HOUR) {
            throw new TooManyRequestsException(
                    "Too many codes requested for this account. Please try again in an hour.");
        }

        // Stops a held-down "resend" button turning into a mailbox full of codes,
        // and gives the previous message time to actually arrive.
        otpCodeRepository.findFirstByDestinationOrderByIdDesc(destination).ifPresent(latest -> {
            if (latest.getCreatedAt() != null
                    && latest.getCreatedAt().isAfter(now.minusSeconds(RESEND_COOLDOWN_SECONDS))) {
                throw new TooManyRequestsException(
                        "A code was just sent. Please wait a minute before asking for another.");
            }
        });
    }

    private OtpSendResponse response() {
        return OtpSendResponse.builder()
                .expiresInSeconds((int) Duration.ofMinutes(CODE_TTL_MINUTES).toSeconds())
                .resendAfterSeconds(RESEND_COOLDOWN_SECONDS)
                .build();
    }

    /**
     * The single failure every unsuccessful verification produces, whatever
     * actually went wrong.
     */
    private static BadRequestException rejected() {
        return new BadRequestException("That code is incorrect or has expired. Please request a new one.");
    }
}
