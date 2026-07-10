package be.ephec.pdw.padel.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message, Instant.now(), Map.of());
    }

    public static ErrorResponse validation(Map<String, String> fieldErrors) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Données invalides", Instant.now(), fieldErrors);
    }
}
