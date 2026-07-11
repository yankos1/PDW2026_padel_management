package be.ephec.pdw.padel.dto;

import java.math.BigDecimal;

public record DashboardSummaryDto(
        BigDecimal chiffreAffaires,
        long nombreMatchs,
        long reservationsConfirmees,
        BigDecimal tauxRemplissageMatchs,
        BigDecimal tauxOccupationTerrains,
        BigDecimal soldesDus,
        long membresActifs,
        long matchsAnnules,
        long matchsProchainsIncomplets
) {
}
