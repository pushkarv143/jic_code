package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What a correct code buys you, which depends on what it was for.
 *
 * For PASSWORD_RESET this is a short-lived single-use token that the existing
 * {@code POST /auth/reset-password} already knows how to consume — the six
 * digits are spent here and never travel again. For LOGIN it would instead
 * carry the usual token pair, so the clients' session handling is unchanged;
 * that purpose has no endpoint wired to it yet.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerifyResponse {

    /** Set for PASSWORD_RESET. Feed straight into /auth/reset-password. */
    private String resetToken;

    /** Set for LOGIN. Null for every other purpose. */
    private JwtAuthResponse auth;
}
