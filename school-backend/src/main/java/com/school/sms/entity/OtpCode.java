package com.school.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A one-time passcode issued to a user for a single {@link OtpPurpose}.
 *
 * Shaped like {@link PasswordResetToken} deliberately, with two differences
 * that matter: the secret is stored hashed rather than in the clear, because
 * six digits is guessable; and it carries an attempt counter, because a code
 * short enough to retype is short enough to brute-force without one.
 *
 * The digits themselves exist only in the message sent to the user — never in
 * this row, never in a log, never in a response body.
 */
@Entity
@Table(name = "otp_codes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "codeHash"})
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Bcrypt hash of the six digits, produced by the same encoder as passwords. */
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private OtpChannel channel;

    /** The address this was sent to, as supplied — used for throttling and auditing. */
    @Column(name = "destination", nullable = false, length = 150)
    private String destination;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Set the moment a code is accepted, which is what makes it single-use. */
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * True once the code is spent, timed out, or has absorbed too many wrong
     * guesses — the three ways a code stops being usable.
     */
    public boolean isUsable(int maxAttempts) {
        return !isConsumed() && !isExpired() && attempts < maxAttempts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OtpCode that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
