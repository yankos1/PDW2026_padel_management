package be.ephec.pdw.padel.seed;

import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.JourFermetureRepository;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DevDataSeederTest {
    private static final String DEFAULT_PASSWORD = "configured-admin-password";

    private final MembreRepository membreRepository = mock(MembreRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createsBcryptHashForAdminWithoutPassword() {
        DevDataSeeder seeder = seeder(DEFAULT_PASSWORD);
        MembreGlobal admin = admin(null);

        seeder.initialiserMotDePasseAdmin(admin);

        assertNotEquals(DEFAULT_PASSWORD, admin.getAdminPasswordHash());
        assertTrue(admin.getAdminPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches(DEFAULT_PASSWORD, admin.getAdminPasswordHash()));
        verify(membreRepository).save(admin);
    }

    @Test
    void neverStoresConfiguredPasswordInPlainText() {
        MembreGlobal admin = admin(null);

        seeder(DEFAULT_PASSWORD).initialiserMotDePasseAdmin(admin);

        assertNotEquals(DEFAULT_PASSWORD, admin.getAdminPasswordHash());
    }

    @Test
    void doesNotReplaceExistingAdminHash() {
        String existingHash = passwordEncoder.encode("already-changed-password");
        MembreGlobal admin = admin(existingHash);

        seeder(DEFAULT_PASSWORD).initialiserMotDePasseAdmin(admin);

        assertEquals(existingHash, admin.getAdminPasswordHash());
        verify(membreRepository, never()).save(admin);
    }

    @Test
    void doesNotSetAdminPasswordOnNormalUser() {
        MembreLibre user = new MembreLibre();
        user.setMatricule("L9000");
        user.setRole(Role.USER);

        seeder(DEFAULT_PASSWORD).initialiserMotDePasseAdmin(user);

        assertNull(user.getAdminPasswordHash());
        verify(membreRepository, never()).save(user);
    }

    @Test
    void emptyConfigurationDoesNotCreateKnownPassword() {
        MembreGlobal admin = admin(null);

        seeder(" ").initialiserMotDePasseAdmin(admin);

        assertNull(admin.getAdminPasswordHash());
        verify(membreRepository, never()).save(admin);
    }

    private DevDataSeeder seeder(String configuredPassword) {
        DevDataSeeder seeder = new DevDataSeeder(
                mock(SiteRepository.class),
                mock(TerrainRepository.class),
                membreRepository,
                mock(JourFermetureRepository.class),
                mock(MatchRepository.class),
                mock(ReservationRepository.class),
                passwordEncoder
        );
        ReflectionTestUtils.setField(seeder, "adminDefaultPassword", configuredPassword);
        return seeder;
    }

    private MembreGlobal admin(String hash) {
        MembreGlobal admin = new MembreGlobal();
        admin.setMatricule("G9000");
        admin.setRole(Role.ADMIN_GLOBAL);
        admin.setAdminPasswordHash(hash);
        return admin;
    }
}
