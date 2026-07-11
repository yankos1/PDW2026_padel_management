package be.ephec.pdw.padel.dto;

import java.math.BigDecimal;

public record TerrainStatisticsDto(
        Long terrainId,
        String terrainNom,
        Long siteId,
        String siteNom,
        long nombreMatchs,
        long reservationsValides,
        BigDecimal tauxRemplissage
) {
}
