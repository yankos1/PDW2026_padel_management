package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
import be.ephec.pdw.padel.dto.LoginDTO;
import be.ephec.pdw.padel.dto.RegisterDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MembreRepository membreRepository;

    public Membre login(LoginDTO input) {
        Membre membre = membreRepository.findById(normalizeMatricule(input.getMatricule()))
                .orElseThrow(() -> new BusinessRuleException("Invalid matricule"));

        if (isAdmin(membre)) {
            validateAdminPassword(membre, input.getPassword());
        }

        return membre;
    }

    public Map<String, Boolean> adminPasswordStatus(String matricule) {
        Membre membre = membreRepository.findById(normalizeMatricule(matricule))
                .orElseThrow(() -> new BusinessRuleException("Invalid matricule"));

        boolean admin = isAdmin(membre);

        return Map.of(
                "admin", admin,
                "passwordCreation", admin && (membre.getAdminPasswordHash() == null || membre.getAdminPasswordHash().isBlank())
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
                .orElse("L0001"); //cas si pas encore de membre libre dans la db
    }

    private String nextLibreMatricule(String currentMatricule) {
        int number = Integer.parseInt(currentMatricule.substring(1));
        return "L" + String.format("%04d", number + 1);
    }

    private boolean isAdmin(Membre membre) {
        return membre.getRole() == Role.ADMIN_GLOBAL || membre.getRole() == Role.ADMIN_SITE;
    }

    private void validateAdminPassword(Membre membre, String password) {
        if (membre.getAdminPasswordHash() == null || membre.getAdminPasswordHash().isBlank()) {
            configureFirstAdminPassword(membre, password);
            return;
        }

        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("Mot de passe admin requis");
        }

        if (!MessageDigest.isEqual(hash(password).getBytes(StandardCharsets.UTF_8),
                membre.getAdminPasswordHash().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessRuleException("Mot de passe admin incorrect");
        }
    }

    private void configureFirstAdminPassword(Membre membre, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("Creation du mot de passe admin requise");
        }

        if (password.length() < 6) {
            throw new BusinessRuleException("Le mot de passe admin doit contenir au moins 6 caracteres");
        }

        membre.setAdminPasswordHash(hash(password));
        membreRepository.save(membre);
    }

    private String hash(String value) {
        try {
            // TODO [IMPORTANT][SECURITE] Remplacer SHA-256 par BCrypt pour le stockage securise des mots de passe.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
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
