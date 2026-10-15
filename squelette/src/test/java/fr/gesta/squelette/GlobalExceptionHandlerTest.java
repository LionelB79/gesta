package fr.gesta.squelette;

import fr.gesta.squelette.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class GlobalExceptionHandlerTest {
    private  GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void init() {
        globalExceptionHandler = new GlobalExceptionHandler();

    }


    @Test
    void handleInternalExceptionTest() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/boom");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleInternalException(new RuntimeException("détail interne sensible"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("Erreur inconnue",response.getBody().message());
        assertEquals("/api/test/boom",response.getBody().path());
        assertFalse(response.getBody().message().contains("détail interne sensible"));

    }

    @Test
    void handleNoResourceTest() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/inconnu");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNoResource(new NoResourceFoundException(HttpMethod.GET, "/api/test/inconnu", "test/inconnu"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().code());
        assertEquals("Élément introuvable",response.getBody().message());
        assertEquals("/api/test/inconnu",response.getBody().path());
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
