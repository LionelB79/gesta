package fr.gesta.squelette;

import fr.gesta.squelette.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class GlobalExceptionHandlerTest {
    private  GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void init() {
        globalExceptionHandler = new GlobalExceptionHandler();

    }
    @Test
    void handleMethodNotSupportedTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/login");
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET", List.of("POST"));

        ResponseEntity<Object> response = globalExceptionHandler.handleException(ex, new ServletWebRequest(request));
        ErrorResponse body = (ErrorResponse) response.getBody();

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("METHOD_NOT_ALLOWED", body.code());
        assertEquals("/api/login", body.path());
        assertEquals(Set.of(HttpMethod.POST), response.getHeaders().getAllow());
    }

    @Test
    void handleMessageNotReadableDoesNotLeakParserDetailsTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/bookings");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected end-of-input", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Object> response = globalExceptionHandler.handleException(ex, new ServletWebRequest(request));
        ErrorResponse body = (ErrorResponse) response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", body.code());
        assertFalse(body.message().contains("JSON parse error"));
    }

    @Test
    void handleInvalidSortTest() {
        PropertyReferenceException ex = new PropertyReferenceException("foo", TypeInformation.of(Object.class), List.of());

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInvalidSort(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("INVALID_SORT", response.getBody().code());
        assertEquals("Paramètre de tri invalide", response.getBody().message());
        assertEquals("/api/bookings", response.getBody().path());
    }

    @Test
    void handleInternalExceptionTest() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/boom");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnexpected(new RuntimeException("détail interne sensible"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("Erreur inconnue",response.getBody().message());
        assertEquals("/api/test/boom",response.getBody().path());
        assertFalse(response.getBody().message().contains("détail interne sensible"));

    }

    @Test
    void handleNoResourceTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/inconnu");
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/test/inconnu", "test/inconnu");

        ResponseEntity<Object> response = globalExceptionHandler.handleException(ex, new ServletWebRequest(request));
        ErrorResponse body = (ErrorResponse) response.getBody();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", body.code());
        assertEquals("Not Found", body.message());
        assertEquals("/api/test/inconnu", body.path());
    }

    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/bookings");

    private void assertResponse(ResponseEntity<ErrorResponse> response, HttpStatus status, String code) {
        assertEquals(status, response.getStatusCode());
        assertEquals(status.value(), response.getBody().status());
        assertEquals(code, response.getBody().code());
        assertEquals("message", response.getBody().message());
        assertEquals("/api/bookings", response.getBody().path());
    }

    @Test
    void handleNotFoundTest() {
        var response = globalExceptionHandler.handleNotFound(new NotFoundException("message", "TEST_NOT_FOUND") {}, request);
        assertResponse(response, HttpStatus.NOT_FOUND, "TEST_NOT_FOUND");
    }

    @Test
    void handleConflictTest() {
        var response = globalExceptionHandler.handleConflict(new ConflictException("message", "TEST_CONFLICT") {}, request);
        assertResponse(response, HttpStatus.CONFLICT, "TEST_CONFLICT");
    }

    @Test
    void handleForbiddenTest() {
        var response = globalExceptionHandler.handleForbidden(new ForbiddenException("message", "TEST_FORBIDDEN") {}, request);
        assertResponse(response, HttpStatus.FORBIDDEN, "TEST_FORBIDDEN");
    }

    @Test
    void handleUnauthorizedTest() {
        var response = globalExceptionHandler.handleUnauthorized(new UnauthenticatedException("message", "TEST_UNAUTHORIZED") {}, request);
        assertResponse(response, HttpStatus.UNAUTHORIZED, "TEST_UNAUTHORIZED");
    }

    @Test
    void handleBusinessRuleTest() {
        var response = globalExceptionHandler.handleBusinessRule(new BusinessRuleException("message", "TEST_RULE") {}, request);
        assertResponse(response, HttpStatus.BAD_REQUEST, "TEST_RULE");
    }

    @Test
    void handleUncategorizedTest() {
        var response = globalExceptionHandler.handleUncategorized(new BusinessException("message", "TEST_RAW") {}, request);
        assertResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "TEST_RAW");
    }
}
