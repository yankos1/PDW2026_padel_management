package be.ephec.pdw.padel;

import be.ephec.pdw.padel.exception.BusinessRuleException;
import be.ephec.pdw.padel.exception.ErrorResponse;
import be.ephec.pdw.padel.exception.ForbiddenException;
import be.ephec.pdw.padel.exception.GlobalExceptionHandler;
import be.ephec.pdw.padel.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnErrorResponseForBusinessRuleException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessRuleException(new BusinessRuleException("Regle invalide"));

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Regle invalide");
    }

    @Test
    void shouldReturnErrorResponseForForbiddenException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleForbiddenException(new ForbiddenException("Acces refuse"));

        assertErrorResponse(response, HttpStatus.FORBIDDEN, "Acces refuse");
    }

    @Test
    void shouldReturnErrorResponseForTooManyLoginAttemptsException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleTooManyLoginAttemptsException(
                        new TooManyLoginAttemptsException("Trop de tentatives echouees. Reessayez plus tard.")
                );

        assertErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives echouees. Reessayez plus tard.");
    }

    @Test
    void shouldReturnErrorResponseForNotFoundResponseStatusException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatusException(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource introuvable"));

        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Ressource introuvable");
    }

    @Test
    void shouldReturnGenericErrorResponseForUnexpectedException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpectedException(new IllegalStateException("SQL password leaked"));

        assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue");
    }

    private void assertErrorResponse(ResponseEntity<ErrorResponse> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status.value());
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }
}
