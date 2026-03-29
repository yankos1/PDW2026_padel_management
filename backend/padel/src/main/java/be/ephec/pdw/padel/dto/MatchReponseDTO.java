package be.ephec.pdw.padel.dto;

import java.time.LocalDateTime;

public record MatchReponseDTO(
        Long id,
        LocalDateTime dateHeureDebut,
        int nbParticipants
) {
}
