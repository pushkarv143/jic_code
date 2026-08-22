package com.school.sms.exception;

import com.school.sms.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the handler says when the request never reached a controller.
 *
 * <p>These three cases share a failure mode: nothing went wrong on the server, but
 * without a handler each one fell through to the catch-all and was reported as
 * "An unexpected error occurred" with a 500 and a logged stack trace. A client
 * cannot act on that, and it hides the real errors in the log.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest requestFor(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    @DisplayName("an unknown path is 404, and names the path so the caller can see its mistake")
    void unknownPathIsNotFound() {
        NoResourceFoundException ex =
                new NoResourceFoundException(HttpMethod.GET, "api/v1/subjects");

        ResponseEntity<ErrorResponse> response =
                handler.handleNoResourceFound(ex, requestFor("GET", "/api/v1/subjects"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .contains("GET")
                .contains("/api/v1/subjects");
        // Not the catch-all's text: that would claim the server had failed.
        assertThat(response.getBody().getMessage()).doesNotContain("unexpected error");
    }

    @Test
    @DisplayName("a wrong verb on a real path is 405, with the permitted verbs echoed")
    void wrongVerbIsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST", Set.of("GET", "PUT"));

        ResponseEntity<ErrorResponse> response =
                handler.handleMethodNotSupported(ex, requestFor("POST", "/api/v1/my-class/students"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.PUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("POST").contains("GET, PUT");
    }
}
