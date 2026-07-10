package be.ephec.pdw.padel;

import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.service.AuthService;
import be.ephec.pdw.padel.service.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void shouldLoginUserWithoutPasswordAndWithoutAuthenticationManager() {
        MembreRepository repository = mock(MembreRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AuthService service = new AuthService(
                repository,
                new BCryptPasswordEncoder(),
                new LoginAttemptService(),
                authenticationManager
        );
        MembreLibre user = new MembreLibre();
        user.setMatricule("G0002");
        user.setRole(Role.USER);
        when(repository.findById("G0002")).thenReturn(Optional.of(user));

        MembreLibre result = (MembreLibre) service.login(login("g0002", null));

        assertEquals("G0002", result.getMatricule());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void shouldRejectUnknownUser() {
        MembreRepository repository = mock(MembreRepository.class);
        AuthService service = new AuthService(
                repository,
                new BCryptPasswordEncoder(),
                new LoginAttemptService(),
                mock(AuthenticationManager.class)
        );
        when(repository.findById("G9999")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> service.login(login("G9999", null)));
    }

    @Test
    void shouldRejectAdminWithoutPassword() {
        MembreRepository repository = mock(MembreRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AuthService service = new AuthService(
                repository,
                new BCryptPasswordEncoder(),
                new LoginAttemptService(),
                authenticationManager
        );
        MembreGlobal admin = admin();
        when(repository.findById("G0001")).thenReturn(Optional.of(admin));

        assertThrows(BadCredentialsException.class, () -> service.login(login("G0001", null)));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void shouldLoginAdminWithGoodPasswordThroughAuthenticationManager() {
        MembreRepository repository = mock(MembreRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AuthService service = new AuthService(
                repository,
                new BCryptPasswordEncoder(),
                new LoginAttemptService(),
                authenticationManager
        );
        MembreGlobal admin = admin();
        when(repository.findById("G0001")).thenReturn(Optional.of(admin));
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("G0001", null));

        var result = service.login(login("G0001", "valid-password"));

        assertEquals("G0001", result.getMatricule());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void shouldRejectAdminWithWrongPassword() {
        MembreRepository repository = mock(MembreRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AuthService service = new AuthService(
                repository,
                new BCryptPasswordEncoder(),
                new LoginAttemptService(),
                authenticationManager
        );
        MembreGlobal admin = admin();
        when(repository.findById("G0001")).thenReturn(Optional.of(admin));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadCredentialsException.class, () -> service.login(login("G0001", "wrong-password")));
    }

    private LoginDTO login(String matricule, String password) {
        LoginDTO input = new LoginDTO();
        input.setMatricule(matricule);
        input.setPassword(password);
        return input;
    }

    private MembreGlobal admin() {
        MembreGlobal admin = new MembreGlobal();
        admin.setMatricule("G0001");
        admin.setRole(Role.ADMIN_GLOBAL);
        admin.setAdminPasswordHash(new BCryptPasswordEncoder().encode("valid-password"));
        return admin;
    }
}
