package be.ephec.pdw.padel.dto;

import java.time.LocalDateTime;

public record IncompleteMatchDto(
        Long matchId,
        LocalDateTime dateHeureDebut,
        Long terrainId,
        String terrainNom,
        Long siteId,
        String siteNom,
        long participants,
        long placesRestantes
) {
}
