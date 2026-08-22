package com.school.sms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtAuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;
    private UserDto user;

    /**
     * True when the caller signed in with a password the system generated and must
     * replace it before anything else will work.
     *
     * <p>On the response rather than left for the client to discover from a 403:
     * both apps need to route straight to the change-password screen, and a client
     * that has to provoke an error first would flash the dashboard on the way.
     * Tokens are still issued — the change-password endpoint needs authenticating
     * like any other — they simply cannot be used for anything else yet.
     */
    private boolean mustChangePassword;
}
