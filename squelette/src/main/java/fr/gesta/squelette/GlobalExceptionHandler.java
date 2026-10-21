package fr.gesta.squelette;

import fr.gesta.squelette.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String pathOf(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }

    // ── Exceptions Spring MVC (circuit hérité) ──
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = pathOf(request);

        String fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse response = new ErrorResponse(Instant.now(), status.value(), "VALIDATION_ERROR", fieldErrors, path);
        return handleExceptionInternal(ex, response, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        String path = pathOf(request);
        log.warn("Requête rejetée sur {} : {}", path, ex.getClass().getSimpleName());

        if (body instanceof ErrorResponse errorResponse) {
            return ResponseEntity.status(statusCode).headers(headers).body(errorResponse);
        }

        String code;
        String message;
        if (httpStatus != null) {
            code = httpStatus.name();
            message = httpStatus.getReasonPhrase();
        } else {
            code = "HTTP_ERROR";
            message = "Requête invalide";
        }

        ErrorResponse response = new ErrorResponse(Instant.now(), statusCode.value(), code, message, path);
        return ResponseEntity.status(statusCode).headers(headers).body(response);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSort(PropertyReferenceException e, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 400, "INVALID_SORT", "Paramètre de tri invalide", request.getRequestURI());
        log.warn("Tri invalide sur {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ── Filet ──
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 500, "INTERNAL_ERROR", "Erreur inconnue", request.getRequestURI());
        log.error("Erreur non gérée sur {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }



    // ── Exceptions métier ──
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
