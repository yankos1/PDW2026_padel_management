package be.ephec.pdw.padel.dto;

import be.ephec.pdw.padel.model.StatutMatch;

import java.time.LocalDateTime;

public record MatchReponseDTO(
        Long id,
        LocalDateTime dateHeureDebut,
        int nbParticipants,
        String terrain,
        Long siteId,
        String site,
        String organisateurMatricule,
        boolean estPublic,
        StatutMatch statut
) {
}
