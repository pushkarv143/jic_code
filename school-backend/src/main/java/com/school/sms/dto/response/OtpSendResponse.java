package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Deliberately says almost nothing.
 *
 * It carries no indication of whether an account existed, which channel was
 * used, or where the code went — all of which would turn this endpoint into a
 * way of testing whether an email address is registered. The response is
 * identical for a real account and an unknown one; only the expiry is stated,
 * because the screen needs it for the resend countdown and it is the same
 * number either way.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpSendResponse {

    /** How long a code is good for, so the client can run its countdown. */
    private int expiresInSeconds;

    /** How long before another code may be requested. */
    private int resendAfterSeconds;
}
