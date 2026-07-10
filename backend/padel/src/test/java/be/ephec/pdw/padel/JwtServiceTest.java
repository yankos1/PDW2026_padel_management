package be.ephec.pdw.padel;

import be.ephec.pdw.padel.security.JwtService;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "test-secret-for-jwt-that-is-at-least-32-bytes";

    @Test
    void shouldGenerateAndValidateToken() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000);
        MembreGlobal membre = new MembreGlobal();
        membre.setMatricule("G0001");
        membre.setRole(Role.ADMIN_GLOBAL);

        String token = jwtService.generateToken(membre);

        JwtService.JwtUser user = jwtService.validateToken(token).orElseThrow();
        assertEquals("G0001", user.matricule());
        assertEquals(Role.ADMIN_GLOBAL, user.role());
    }

    @Test
    void shouldRejectMalformedToken() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000);

        assertTrue(jwtService.validateToken("not-a-jwt").isEmpty());
    }

    @Test
    void shouldRejectWrongSignature() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000);
        JwtService otherJwtService = new JwtService("another-test-secret-with-at-least-32-bytes", 3_600_000);
        MembreGlobal membre = new MembreGlobal();
        membre.setMatricule("G0001");
        membre.setRole(Role.ADMIN_GLOBAL);

        String token = jwtService.generateToken(membre);

        assertTrue(otherJwtService.validateToken(token).isEmpty());
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1);
        MembreGlobal membre = new MembreGlobal();
        membre.setMatricule("G0001");
        membre.setRole(Role.ADMIN_GLOBAL);

        String token = jwtService.generateToken(membre);
        Thread.sleep(5);

        assertTrue(jwtService.validateToken(token).isEmpty());
    }
}
