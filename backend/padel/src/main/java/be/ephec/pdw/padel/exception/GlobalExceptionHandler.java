package be.ephec.pdw.padel.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(BusinessRuleException e) {
        log.warn("Business rule violated: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e) {
        log.warn("Forbidden access: {}", e.getMessage());
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            String message = error.getDefaultMessage();
            if (message == null || message.isBlank()) {
                message = "Valeur invalide";
            }
            fieldErrors.putIfAbsent(error.getField(), message);
        });

        log.warn("Validation failed: {}", fieldErrors);
        return new ResponseEntity<>(ErrorResponse.validation(fieldErrors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Authentication failed");
        return error(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.warn("Authentication failed");
        return error(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyLoginAttemptsException(TooManyLoginAttemptsException e) {
        log.warn("Login blocked: {}", e.getMessage());
        return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("Unexpected response status exception", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue");
        }

        String message = e.getReason();
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        }

        log.warn("Response status exception: {} {}", status.value(), message);
        return error(status, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return new ResponseEntity<>(ErrorResponse.of(status, message), status);
    }
}
