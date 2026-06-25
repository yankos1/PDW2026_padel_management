package be.ephec.pdw.padel.dto;

import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.MembreSite;
import be.ephec.pdw.padel.model.Role;

import java.time.LocalDateTime;

public record SessionDTO(
        String matricule,
        String nom,
        String prenom,
        String email,
        String typeMembre,
        Role role,
        boolean penaliteActive,
        LocalDateTime finPenalite,
        double soldeDu,
        SiteDTO site
) {
    public static SessionDTO from(Membre membre) {
        return new SessionDTO(
                membre.getMatricule(),
                membre.getNom(),
                membre.getPrenom(),
                membre.getEmail(),
                typeMembre(membre),
                membre.getRole(),
                membre.isPenaliteActive(),
                membre.getFinPenalite(),
                membre.getSoldeDu(),
                site(membre)
        );
    }

    private static String typeMembre(Membre membre) {
        if (membre instanceof MembreGlobal) {
            return "GLOBAL";
        }

        if (membre instanceof MembreSite) {
            return "SITE";
        }

        if (membre instanceof MembreLibre) {
            return "LIBRE";
        }

        return null;
    }

    private static SiteDTO site(Membre membre) {
        if (membre instanceof MembreSite membreSite && membreSite.getSite() != null) {
            return SiteDTO.from(membreSite.getSite());
        }

        return null;
    }
}
