package com.school.sms.exception;

import com.school.sms.dto.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse errorResponse = buildError(HttpStatus.BAD_REQUEST, "Validation failed", request);
        errorResponse.setValidationErrors(validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
        return respond(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefresh(TokenRefreshException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "Your account is not active. Please contact the school administrator.", request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "Your account has been locked. Please contact the school administrator.", request);
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<ErrorResponse> handleAccountExpired(AccountExpiredException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "Your account has expired. Please contact the school administrator.", request);
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<ErrorResponse> handleCredentialsExpired(CredentialsExpiredException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "Your credentials have expired. Please reset your password.", request);
    }

    /**
     * A {@code @PreAuthorize} refusal — the caller's role/permission does not admit
     * them to this endpoint at all.
     *
     * <p>Answered with a deliberately generic message. Spring's own text ("Access
     * Denied") says nothing useful, and spelling out which authority was wanted
     * would describe the policy to someone who has just been told they are outside
     * it.
     *
     * <p>Declared separately from {@link #handleAccessDenied} because
     * {@code AuthorizationDeniedException} extends {@code AccessDeniedException}:
     * Spring dispatches to the most specific handler, so method-security refusals
     * land here and the domain-level ones below keep their own wording.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex,
                                                                  HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request);
    }

    /**
     * A row-level refusal thrown by one of the guards — the caller may use this
     * endpoint, but not on this class, section or student.
     *
     * <p>These messages are written for the person reading them and are passed
     * through: "You are not the class teacher of any section" and "That student is
     * not in your class" tell a teacher what to do next, where the generic text
     * above leaves them assuming the feature is broken. That distinction is the
     * whole point of {@code HomeroomGuard}, {@code SectionAccessGuard} and
     * {@code StudentAccessGuard} carrying their own wording, and it was being
     * discarded here — every one of them arrived at the client as "You do not have
     * permission to perform this action".
     *
     * <p>Safe to expose: the guards are careful not to distinguish "does not exist"
     * from "not yours", so nothing here confirms the existence of another section's
     * records. Falls back to the generic message if a guard throws without one.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String message = StringUtils.hasText(ex.getMessage())
                ? ex.getMessage()
                : "You do not have permission to perform this action";
        return respond(HttpStatus.FORBIDDEN, message, request);
    }

    /**
     * The right URL with the wrong verb — 405, not 500.
     *
     * <p>Spring raises this itself, and with no handler for it the catch-all below
     * turned it into "An unexpected error occurred", which is both a lie and a
     * misleading one: nothing failed, the route simply does not accept that method.
     * It showed up when {@code POST /api/v1/my-class/students} was removed — a
     * client still calling it was told the server had broken rather than that
     * admissions had moved.
     *
     * <p>The permitted methods are echoed in the {@code Allow} header, which the
     * spec requires on a 405, and named in the message so a human reading the
     * response body does not have to go looking for the header.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        Set<HttpMethod> allowed = ex.getSupportedHttpMethods();
        String supported = allowed == null || allowed.isEmpty()
                ? ""
                : " Supported: " + allowed.stream().map(HttpMethod::name).sorted().collect(Collectors.joining(", "))
                        + ".";

        ErrorResponse body = buildError(HttpStatus.METHOD_NOT_ALLOWED,
                ex.getMethod() + " is not supported on this endpoint." + supported, request);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (allowed != null && !allowed.isEmpty()) {
            response.allow(allowed.toArray(HttpMethod[]::new));
        }
        return response.body(body);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(ExpiredJwtException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "JWT token has expired", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return respond(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
    }

    /**
     * A multipart request missing a required part is a client mistake, not a server
     * fault. Without this it falls through to the catch-all below and answers 500,
     * which tells the caller nothing about what to fix — every multipart endpoint
     * (student photos and documents, assignments, study materials) is affected.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex,
                                                            HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST,
                "Required request part '" + ex.getRequestPartName() + "' is missing", request);
    }

    /** Likewise for a missing query/form parameter. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                 HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return respond(HttpStatus.CONFLICT, "The request could not be completed due to a data conflict", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing request {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", request);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(buildError(status, message, request));
    }

    private ErrorResponse buildError(HttpStatus status, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }
}
