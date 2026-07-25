package fr.gesta.squelette;

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
    void handleApiExceptionTest() {

        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/boom");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleApiException(new ApiException("erreur metier", HttpStatus.BAD_REQUEST, "BAD_REQUEST"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().code());
        assertEquals("erreur metier",response.getBody().message());
        assertEquals("/api/test/boom",response.getBody().path());


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
    void handleNotFoundExceptionTest() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/inconnu");
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleNotFoundException(new NoResourceFoundException(HttpMethod.GET, "/api/test/inconnu", "test/inconnu"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().code());
        assertEquals("élément introuvable",response.getBody().message());
        assertEquals("/api/test/inconnu",response.getBody().path());
    }
}
