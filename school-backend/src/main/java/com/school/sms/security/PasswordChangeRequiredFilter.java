package com.school.sms.security;

import com.school.sms.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Refuses everything except changing the password, for an account still on the
 * password the system generated for it.
 *
 * <h2>Why a filter</h2>
 *
 * <p>The alternative is a check in every controller, and the failure mode of that
 * is a controller somebody forgets — which is precisely the endpoint an unfinished
 * account should not reach. One filter in front of the whole API cannot be
 * forgotten, and it fails closed: an account with the flag set can do nothing until
 * the flag is cleared.
 *
 * <p>It runs <em>after</em> authentication, deliberately. It needs to know who the
 * caller is, and an unauthenticated request is not its business — Spring Security
 * has already dealt with that.
 *
 * <p>The flag is read off the principal rather than queried, and that is not a
 * staleness compromise: {@code JwtAuthenticationFilter} rebuilds the principal from
 * the database on every request, so it is exactly as current as a query would be
 * and costs nothing extra. It also keeps this filter free of a repository, which
 * matters because it sits in the security chain that the controller slice tests
 * assemble without a persistence layer.
 *
 * <h2>What stays open</h2>
 *
 * <p>Only what the reset itself needs, and the way out:
 *
 * <ul>
 *   <li>{@code POST /api/v1/auth/change-password} — the point of the exercise</li>
 *   <li>{@code POST /api/v1/auth/logout} — a user who signed in on the wrong
 *       account must be able to leave without first changing its password</li>
 *   <li>{@code GET /api/v1/auth/me} — both clients read this to render the shell
 *       around the change-password screen; refusing it would leave them unable to
 *       show whose account they are resetting</li>
 * </ul>
 *
 * <p>Note {@code forgot-password} and the OTP endpoints are not listed: they are
 * unauthenticated, so they never reach this filter. That matters — signing in with
 * a one-time code lands here exactly like a password login, so the code cannot be
 * used to skip the reset.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/auth/change-password",
            "/api/v1/auth/logout",
            "/api/v1/auth/me");

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal().orElse(null);
        if (principal == null
                || !principal.isMustChangePassword()
                || ALLOWED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Blocked {} {} — account {} is still on a generated password",
                request.getMethod(), request.getRequestURI(), principal.getId());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                // Named so a client that reaches here without checking the login
                // response can still route correctly rather than showing a bare 403.
                .message("Your password must be changed before you can use the app. "
                        + "PASSWORD_CHANGE_REQUIRED")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
