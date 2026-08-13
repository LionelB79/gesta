package fr.gesta.squelette;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), e.getStatus().value(), e.getCode(), e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(e.getStatus()).body(response);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalException(Exception e, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 500, "INTERNAL_ERROR", "Erreur inconnue", request.getRequestURI());
        log.error("Erreur non gérée sur {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NoResourceFoundException e, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 404, "NOT_FOUND", "Élément introuvable", request.getRequestURI());
        log.warn("Ressource introuvable : {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String fieldErrors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse response = new ErrorResponse(Instant.now(), 400, "VALIDATION_ERROR", fieldErrors, request.getRequestURI());
        log.warn("Demande non valide sur {} : {}",request.getRequestURI(), fieldErrors);


        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
