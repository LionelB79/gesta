package fr.gesta.squelette;

import fr.gesta.squelette.exception.*;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInternalException(Exception e, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 500, "INTERNAL_ERROR", "Erreur inconnue", request.getRequestURI());
        log.error("Erreur non gérée sur {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
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
        log.warn("Demande non valide sur {} : {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e, HttpServletRequest request) {
        return buildResponse(e, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e, HttpServletRequest request) {
        return buildResponse(e, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e, HttpServletRequest request) {
        return buildResponse(e, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthenticatedException e, HttpServletRequest request) {
        return buildResponse(e, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException e, HttpServletRequest request) {
        return buildResponse(e, HttpStatus.BAD_REQUEST, request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(BusinessException e, HttpStatus status, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), status.value(), e.getCode(), e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleUncategorized(BusinessException e, HttpServletRequest request) {
        log.error("Exception métier sans catégorie sur {}", request.getRequestURI(), e);
        return buildResponse(e, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

}
