package be.ephec.pdw.padel.dto;

import be.ephec.pdw.padel.model.StatutMatch;

public record MatchStatusStatisticsDto(
        StatutMatch statut,
        long nombre
) {
}
