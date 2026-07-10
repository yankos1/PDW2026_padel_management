package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.exception.TooManyLoginAttemptsException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int BLOCK_MINUTES = 15;

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public void assertNotBlocked(String matricule) {
        String key = normalize(matricule);
        LoginAttempt attempt = attempts.get(key);

        if (attempt == null || attempt.blockedUntil == null) {
            return;
        }

        if (attempt.blockedUntil.isAfter(LocalDateTime.now())) {
            throw new TooManyLoginAttemptsException("Trop de tentatives echouees. Reessayez plus tard.");
        }

        attempts.remove(key);
    }

    public void loginSucceeded(String matricule) {
        attempts.remove(normalize(matricule));
    }

    public void loginFailed(String matricule) {
        attempts.compute(normalize(matricule), (key, current) -> {
            int failedAttempts = current == null ? 1 : current.failedAttempts + 1;
            LocalDateTime blockedUntil = failedAttempts >= MAX_FAILED_ATTEMPTS
                    ? LocalDateTime.now().plusMinutes(BLOCK_MINUTES)
                    : null;

            return new LoginAttempt(failedAttempts, blockedUntil);
        });
    }

    private String normalize(String matricule) {
        return matricule == null ? "" : matricule.trim().toUpperCase();
    }

    private record LoginAttempt(int failedAttempts, LocalDateTime blockedUntil) {
    }
}
