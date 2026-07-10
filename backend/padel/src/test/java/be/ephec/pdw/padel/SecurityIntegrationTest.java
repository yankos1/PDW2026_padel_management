package be.ephec.pdw.padel;

import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    private static final String SECRET = "test-secret-for-jwt-that-is-at-least-32-bytes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        membreRepository.save(membre("G0002", Role.USER, null));
        membreRepository.save(membre("G0003", Role.USER, null));
        membreRepository.save(membre("G0001", Role.ADMIN_GLOBAL, passwordEncoder.encode("valid-password")));
    }

    @Test
    void shouldLoginUserWithoutPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"matricule\":\"G0002\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAcceptJwtForOwnReservations() throws Exception {
        mockMvc.perform(get("/reservation/membre/G0002")
                        .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectOtherMemberReservationsWithForbidden() throws Exception {
        mockMvc.perform(get("/reservation/membre/G0003")
                        .header("Authorization", bearer(userToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectReservationsWithoutJwt() throws Exception {
        mockMvc.perform(get("/reservation/membre/G0002"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectReservationsWithInvalidJwt() throws Exception {
        mockMvc.perform(get("/reservation/membre/G0002")
                        .header("Authorization", bearer("invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectReservationsWithExpiredJwt() throws Exception {
        JwtService shortLivedJwtService = new JwtService(SECRET, 1);
        String expiredToken = shortLivedJwtService.generateToken(membre("G0002", Role.USER, null));
        Thread.sleep(5);

        mockMvc.perform(get("/reservation/membre/G0002")
                        .header("Authorization", bearer(expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAvailableMatchesWithValidJwt() throws Exception {
        mockMvc.perform(get("/match/disponibles")
                        .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk());
    }

    private String userToken() {
        return jwtService.generateToken(membre("G0002", Role.USER, null));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private MembreGlobal membre(String matricule, Role role, String adminPasswordHash) {
        MembreGlobal membre = new MembreGlobal();
        membre.setMatricule(matricule);
        membre.setNom("Test");
        membre.setPrenom("User");
        membre.setEmail(matricule + "@example.test");
        membre.setRole(role);
        membre.setAdminPasswordHash(adminPasswordHash);
        return membre;
    }
}
