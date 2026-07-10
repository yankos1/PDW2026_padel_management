package be.ephec.pdw.padel;

import be.ephec.pdw.padel.exception.TooManyLoginAttemptsException;
import be.ephec.pdw.padel.service.LoginAttemptService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    @Test
    void shouldBlockAfterFiveFailedAttempts() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 5; i++) {
            service.loginFailed("g0001");
        }

        assertThrows(TooManyLoginAttemptsException.class, () -> service.assertNotBlocked("G0001"));
    }

    @Test
    void shouldResetAttemptsAfterSuccessfulLogin() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 4; i++) {
            service.loginFailed("G0001");
        }
        service.loginSucceeded("g0001");

        assertDoesNotThrow(() -> service.assertNotBlocked("G0001"));
    }
}
