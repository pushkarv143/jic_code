package com.school.sms.repository;

import com.school.sms.entity.OtpCode;
import com.school.sms.entity.OtpPurpose;
import com.school.sms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    /**
     * The one code a verification may be checked against: newest first, so an
     * older code can never be used once a newer one has been requested.
     */
    Optional<OtpCode> findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIdDesc(User user, OtpPurpose purpose);

    /**
     * Retires every live code for this user and purpose. Called before issuing a
     * new one so exactly one code is ever valid at a time — otherwise requesting
     * a fresh code would widen the guessing surface instead of resetting it.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OtpCode o SET o.consumedAt = :now "
            + "WHERE o.user = :user AND o.purpose = :purpose AND o.consumedAt IS NULL")
    int consumeOutstanding(@Param("user") User user,
                           @Param("purpose") OtpPurpose purpose,
                           @Param("now") LocalDateTime now);

    /** Send throttling: how many codes this destination has been sent recently. */
    long countByDestinationAndCreatedAtAfter(String destination, LocalDateTime since);

    /** The most recent code sent anywhere to this destination, used for the resend cooldown. */
    Optional<OtpCode> findFirstByDestinationOrderByIdDesc(String destination);

    /** Housekeeping for the scheduled purge of long-dead rows. */
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
