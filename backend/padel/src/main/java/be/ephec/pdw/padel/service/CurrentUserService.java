package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.exception.ForbiddenException;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.repositories.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final MembreRepository membreRepository;

    public Membre getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Utilisateur non authentifie");
        }

        String matricule = authentication.getPrincipal() instanceof UserDetails userDetails
                ? userDetails.getUsername()
                : String.valueOf(authentication.getPrincipal());
        return membreRepository.findById(matricule)
                .orElseThrow(() -> new ForbiddenException("Utilisateur inconnu"));
    }

    public boolean isAdmin(Membre membre) {
        return membre.getRole() == Role.ADMIN_GLOBAL || membre.getRole() == Role.ADMIN_SITE;
    }
}
