package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.exception.BusinessRuleException;
import be.ephec.pdw.padel.exception.ForbiddenException;
import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.dto.RegisterDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final MembreRepository membreRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            MembreRepository membreRepository,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService,
            AuthenticationManager authenticationManager
    ) {
        this.membreRepository = membreRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.authenticationManager = authenticationManager;
    }

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

            if (!isAdmin(membre) || hasNoAdminPassword(membre)
                    || input.getPassword() == null || input.getPassword().isBlank()) {
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

    @Transactional
    public void changeAdminPassword(String matricule, String currentPassword, String newPassword, String confirmation) {
        Membre membre = membreRepository.findById(matricule)
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        if (!isAdmin(membre)) {
            throw new ForbiddenException("Acces reserve aux administrateurs");
        }
        if (hasNoAdminPassword(membre)
                || currentPassword == null
                || !passwordEncoder.matches(currentPassword, membre.getAdminPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        if (newPassword == null || newPassword.isBlank() || !newPassword.equals(confirmation)) {
            throw new BusinessRuleException("Les mots de passe ne correspondent pas");
        }
        if (newPassword.length() < 12) {
            throw new BusinessRuleException("Le mot de passe admin doit contenir au moins 12 caracteres");
        }
        if (passwordEncoder.matches(newPassword, membre.getAdminPasswordHash())) {
            throw new BusinessRuleException("Le nouveau mot de passe doit etre different de l'ancien");
        }

        membre.setAdminPasswordHash(passwordEncoder.encode(newPassword));
        membreRepository.save(membre);
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
