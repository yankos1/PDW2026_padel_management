package be.ephec.pdw.padel.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //TODO amélioration : erreur actuellement retourné sous forme de texte : retourner un objet { code, message, timestamp }
    @ExceptionHandler(BusinessRuleException.class)
    // TODO [IMPORTANT] Retourner une erreur JSON structuree avec code, message et timestamp.
    public ResponseEntity<String> handleBusinessRuleException(BusinessRuleException e) {

        log.warn("Business rule violated: {}", e.getMessage());

        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbiddenException(ForbiddenException e) {

        log.warn("Forbidden access: {}", e.getMessage());

        return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
    }
}
