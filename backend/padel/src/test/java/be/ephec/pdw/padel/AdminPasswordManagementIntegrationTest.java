package be.ephec.pdw.padel;

import be.ephec.pdw.padel.dto.ChangeAdminPasswordDTO;
import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPasswordManagementIntegrationTest {
    private static final String INITIAL_PASSWORD = "initial-admin-password";
    private static final String NEW_PASSWORD = "new-secure-admin-password";
    private static final List<String> TEST_MATRICULES = List.of(
            "G4100", "G4101", "G4102", "G4103", "G4104", "G4105",
            "G4106", "G4107", "G4108", "G4109", "L4100", "L4101"
    );

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private MembreRepository membreRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanTestAccounts() {
        TEST_MATRICULES.forEach(matricule -> membreRepository.findById(matricule)
                .ifPresent(membreRepository::delete));
    }

    @Test
    void adminWithConfiguredPasswordGetsNormalSessionAndNormalJwt() throws Exception {
        saveAdmin("G4100", INITIAL_PASSWORD);

        MvcResult result = login("G4100", INITIAL_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricule").value("G4100"))
                .andExpect(jsonPath("$.role").value("ADMIN_GLOBAL"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = responseJson(result).get("token").asText();
        assertEquals(Role.ADMIN_GLOBAL, jwtService.validateToken(token).orElseThrow().role());
    }

    @Test
    void wrongAdminPasswordIsRejectedGenerically() throws Exception {
        saveAdmin("G4101", INITIAL_PASSWORD);

        login("G4101", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void adminWithoutPasswordIsRejected() throws Exception {
        saveAdmin("G4102", INITIAL_PASSWORD);

        login("G4102", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void adminWithoutHashIsRejected() throws Exception {
        saveAdminWithoutHash("G4103");

        login("G4103", INITIAL_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void normalUserStillLogsInWithMatriculeOnly() throws Exception {
        saveUser("L4100");

        login("L4100", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginNeverChangesAdminPasswordHash() throws Exception {
        MembreGlobal admin = saveAdmin("G4104", INITIAL_PASSWORD);
        String hashBeforeLogin = admin.getAdminPasswordHash();

        login("G4104", INITIAL_PASSWORD).andExpect(status().isOk());

        assertEquals(hashBeforeLogin, reload("G4104").getAdminPasswordHash());
    }

    @Test
    void authenticatedAdminChangesPasswordAndOnlyBcryptHashIsStored() throws Exception {
        MembreGlobal admin = saveAdmin("G4105", INITIAL_PASSWORD);

        changePassword(jwtService.generateToken(admin), INITIAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isNoContent());

        String hash = reload("G4105").getAdminPasswordHash();
        assertNotEquals(NEW_PASSWORD, hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, hash));
        assertFalse(passwordEncoder.matches(INITIAL_PASSWORD, hash));
    }

    @Test
    void oldPasswordIsRejectedAndNewPasswordLogsInAfterChange() throws Exception {
        MembreGlobal admin = saveAdmin("G4106", INITIAL_PASSWORD);
        changePassword(jwtService.generateToken(admin), INITIAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isNoContent());

        login("G4106", INITIAL_PASSWORD).andExpect(status().isUnauthorized());
        login("G4106", NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        MembreGlobal admin = saveAdmin("G4107", INITIAL_PASSWORD);

        changePassword(jwtService.generateToken(admin), "wrong-current-password", NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
        assertTrue(passwordEncoder.matches(INITIAL_PASSWORD, reload("G4107").getAdminPasswordHash()));
    }

    @Test
    void differentConfirmationIsRejected() throws Exception {
        MembreGlobal admin = saveAdmin("G4108", INITIAL_PASSWORD);

        changePassword(jwtService.generateToken(admin), INITIAL_PASSWORD, NEW_PASSWORD, "different-password")
                .andExpect(status().isBadRequest());
        assertTrue(passwordEncoder.matches(INITIAL_PASSWORD, reload("G4108").getAdminPasswordHash()));
    }

    @Test
    void shortNewPasswordIsRejectedByValidation() throws Exception {
        MembreGlobal admin = saveAdmin("G4109", INITIAL_PASSWORD);

        changePassword(jwtService.generateToken(admin), INITIAL_PASSWORD, "short", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void normalUserCannotChangeAdminPassword() throws Exception {
        MembreLibre user = saveUser("L4101");

        changePassword(jwtService.generateToken(user), "anything", NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isForbidden());
        assertNull(reload("L4101").getAdminPasswordHash());
    }

    @Test
    void passwordChangeWithoutJwtIsRejected() throws Exception {
        changePassword(null, INITIAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeDtoCannotTargetAnotherMatricule() {
        assertFalse(Arrays.stream(ChangeAdminPasswordDTO.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("matricule")));
    }

    private org.springframework.test.web.servlet.ResultActions login(String matricule, String password) throws Exception {
        LoginDTO input = new LoginDTO();
        input.setMatricule(matricule);
        input.setPassword(password);
        return mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(input)));
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(
            String token,
            String currentPassword,
            String newPassword,
            String confirmPassword
    ) throws Exception {
        var request = put("/auth/password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(
                        new PasswordInput(currentPassword, newPassword, confirmPassword)
                ));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private MembreGlobal saveAdmin(String matricule, String password) {
        MembreGlobal admin = saveAdminWithoutHash(matricule);
        admin.setAdminPasswordHash(passwordEncoder.encode(password));
        return membreRepository.saveAndFlush(admin);
    }

    private MembreGlobal saveAdminWithoutHash(String matricule) {
        MembreGlobal admin = new MembreGlobal();
        fill(admin, matricule, Role.ADMIN_GLOBAL);
        return membreRepository.saveAndFlush(admin);
    }

    private MembreLibre saveUser(String matricule) {
        MembreLibre user = new MembreLibre();
        fill(user, matricule, Role.USER);
        return membreRepository.saveAndFlush(user);
    }

    private void fill(Membre membre, String matricule, Role role) {
        membre.setMatricule(matricule);
        membre.setNom("Nom");
        membre.setPrenom("Prenom");
        membre.setEmail(matricule + "@example.test");
        membre.setRole(role);
    }

    private Membre reload(String matricule) {
        return membreRepository.findById(matricule).orElseThrow();
    }

    private record PasswordInput(String currentPassword, String newPassword, String confirmPassword) {
    }
}
