package be.ephec.pdw.padel;

import be.ephec.pdw.padel.security.CustomUserDetailsService;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    @Test
    void shouldLoadAdminWithBcryptPasswordAndRoleAuthority() {
        MembreRepository repository = mock(MembreRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        MembreGlobal admin = new MembreGlobal();
        admin.setMatricule("G0001");
        admin.setRole(Role.ADMIN_GLOBAL);
        admin.setAdminPasswordHash(passwordEncoder.encode("valid-password"));
        when(repository.findById("G0001")).thenReturn(Optional.of(admin));

        var service = new CustomUserDetailsService(repository);
        var userDetails = service.loadUserByUsername("g0001");

        assertEquals("G0001", userDetails.getUsername());
        assertTrue(passwordEncoder.matches("valid-password", userDetails.getPassword()));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN_GLOBAL")));
    }

    @Test
    void shouldRejectMemberWithoutRole() {
        MembreRepository repository = mock(MembreRepository.class);
        MembreLibre membre = new MembreLibre();
        membre.setMatricule("L0001");
        when(repository.findById("L0001")).thenReturn(Optional.of(membre));

        var service = new CustomUserDetailsService(repository);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("L0001"));
    }

    @Test
    void shouldRejectUserRoleBecauseOnlyAdminsUseAuthenticationManager() {
        MembreRepository repository = mock(MembreRepository.class);
        MembreLibre membre = new MembreLibre();
        membre.setMatricule("L0001");
        membre.setRole(Role.USER);
        when(repository.findById("L0001")).thenReturn(Optional.of(membre));

        var service = new CustomUserDetailsService(repository);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("L0001"));
    }
}
