package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.exception.ForbiddenException;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreSite;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.model.Site;
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

    public boolean isAdminGlobal(Membre membre) {
        return membre.getRole() == Role.ADMIN_GLOBAL;
    }

    public boolean isAdminSite(Membre membre) {
        return membre.getRole() == Role.ADMIN_SITE;
    }

    public boolean peutAdministrerSite(Membre membre, Site site) {
        if (isAdminGlobal(membre)) {
            return true;
        }
        if (!isAdminSite(membre) || !(membre instanceof MembreSite membreSite)) {
            return false;
        }

        Site siteAdministrateur = membreSite.getSite();
        if (siteAdministrateur == null || site == null) {
            return false;
        }
        if (siteAdministrateur.getId() != null && site.getId() != null) {
            return siteAdministrateur.getId().equals(site.getId());
        }
        return siteAdministrateur == site;
    }

    public void verifierAccesAdministrateurAuSite(Membre membre, Site site) {
        if (!peutAdministrerSite(membre, site)) {
            throw new ForbiddenException("Acces refuse");
        }
    }
}
