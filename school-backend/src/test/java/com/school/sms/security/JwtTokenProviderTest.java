package com.school.sms.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "MHpUaGlzSXNBRGV2RGVmYXVsdFNlY3JldEtleUZvckpXVFNpZ25pbmdQdXJwb3Nlc09ubHlDaGFuZ2VJblByb2R1Y3Rpb25FbnZpcm9ubWVudA==");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", 604800000L);
        jwtTokenProvider.init();
    }

    @Test
    void generatesAndValidatesAccessToken() {
        UserPrincipal principal = new UserPrincipal(1L, "admin", "admin@school.edu", "hash", true,
                "SUPER_ADMIN", Set.of(),
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("admin", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(1L, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void rejectsTamperedToken() {
        UserPrincipal principal = new UserPrincipal(1L, "admin", "admin@school.edu", "hash", true,
                "SUPER_ADMIN", Set.of(),
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        String token = jwtTokenProvider.generateAccessToken(principal);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertTrue(!jwtTokenProvider.validateToken(tampered));
    }
}
