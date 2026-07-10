package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.exception.BusinessRuleException;
import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.dto.RegisterDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MembreRepository membreRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthenticationManager authenticationManager;

    public Membre login(LoginDTO input) {
        String matricule = normalizeMatricule(input.getMatricule());
        loginAttemptService.assertNotBlocked(matricule);

        try {
            Membre membre = membreRepository.findById(matricule)
                    .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

            if (membre.getRole() == null) {
                throw new BadCredentialsException("Identifiants invalides");
            }

            if (membre.getRole() == Role.USER) {
                loginAttemptService.loginSucceeded(matricule);
                return membre;
            }

            if (isAdmin(membre) && hasNoAdminPassword(membre)) {
                configureFirstAdminPassword(membre, input.getPassword());
            }

            if (input.getPassword() == null || input.getPassword().isBlank()) {
                throw new BadCredentialsException("Identifiants invalides");
            }

            authenticate(matricule, input.getPassword());
            loginAttemptService.loginSucceeded(matricule);
            return membre;
        } catch (BusinessRuleException | BadCredentialsException e) {
            loginAttemptService.loginFailed(matricule);
            throw new BadCredentialsException("Identifiants invalides");
        }
    }

    public Map<String, Boolean> adminPasswordStatus(String matricule) {
        Membre membre = membreRepository.findById(normalizeMatricule(matricule))
                .orElseThrow(() -> new BusinessRuleException("Invalid matricule"));

        boolean admin = isAdmin(membre);

        return Map.of(
                "admin", admin,
                "passwordCreation", admin && hasNoAdminPassword(membre)
        );
    }

    public Membre register(RegisterDTO input) {
        MembreLibre membre = new MembreLibre();
        membre.setMatricule(generateLibreMatricule());
        membre.setNom(required(input.getNom(), "Nom requis"));
        membre.setPrenom(required(input.getPrenom(), "Prenom requis"));
        membre.setEmail(required(input.getEmail(), "Email requis"));
        membre.setRole(Role.USER);

        return membreRepository.save(membre);
    }

    private String generateLibreMatricule() {
        return membreRepository
                .findTopByMatriculeStartingWithOrderByMatriculeDesc("L")
                .map(Membre::getMatricule)
                .map(this::nextLibreMatricule)
                .orElse("L0001");//cas si pas encore de membre libre dans la db
    }

    private String nextLibreMatricule(String currentMatricule) {
        int number = Integer.parseInt(currentMatricule.substring(1));
        return "L" + String.format("%04d", number + 1);
    }

    private boolean isAdmin(Membre membre) {
        return membre.getRole() == Role.ADMIN_GLOBAL || membre.getRole() == Role.ADMIN_SITE;
    }

    private void configureFirstAdminPassword(Membre membre, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("Creation du mot de passe admin requise");
        }

        if (password.length() < 12) {
            throw new BusinessRuleException("Le mot de passe admin doit contenir au moins 12 caracteres");
        }

        membre.setAdminPasswordHash(passwordEncoder.encode(password));
        membreRepository.save(membre);
    }

    private void authenticate(String matricule, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(matricule, password == null ? "" : password)
        );
    }

    private boolean hasNoAdminPassword(Membre membre) {
        return membre.getAdminPasswordHash() == null || membre.getAdminPasswordHash().isBlank();
    }

    private String normalizeMatricule(String matricule) {
        if (matricule == null || matricule.isBlank()) {
            throw new BusinessRuleException("Matricule requis");
        }

        return matricule.trim().toUpperCase();
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(message);
        }

        return value.trim();
    }
}
