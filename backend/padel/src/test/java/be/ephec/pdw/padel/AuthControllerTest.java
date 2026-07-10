package be.ephec.pdw.padel;

import be.ephec.pdw.padel.controllers.AuthController;
import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.exception.GlobalExceptionHandler;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.security.JwtService;
import be.ephec.pdw.padel.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthService authService;
    private JwtService jwtService;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        authService = mock(AuthService.class);
        jwtService = mock(JwtService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, jwtService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn200ForExistingUserWithoutPassword() throws Exception {
        MembreLibre user = new MembreLibre();
        user.setMatricule("G0002");
        user.setRole(Role.USER);
        when(authService.login(any(LoginDTO.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-user");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login("G0002", null))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401ForUnknownUser() throws Exception {
        when(authService.login(any(LoginDTO.class))).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login("G9999", null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Identifiants invalides"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void shouldReturn401ForAdminWithoutPassword() throws Exception {
        when(authService.login(any(LoginDTO.class))).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login("G0001", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200ForAdminWithGoodPassword() throws Exception {
        MembreGlobal admin = new MembreGlobal();
        admin.setMatricule("G0001");
        admin.setRole(Role.ADMIN_GLOBAL);
        when(authService.login(any(LoginDTO.class))).thenReturn(admin);
        when(jwtService.generateToken(admin)).thenReturn("jwt-admin");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login("G0001", "valid-password"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401ForAdminWithWrongPassword() throws Exception {
        when(authService.login(any(LoginDTO.class))).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login("G0001", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnValidationFieldErrorsForInvalidLoginInput() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login("INVALID", null))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Données invalides"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors.matricule").value("Matricule invalide"));
    }

    private LoginDTO login(String matricule, String password) {
        LoginDTO input = new LoginDTO();
        input.setMatricule(matricule);
        input.setPassword(password);
        return input;
    }
}
