package be.ephec.pdw.padel.dto;

import java.util.List;

public record AdminDashboardDto(
        DashboardSummaryDto resume,
        List<RevenuePeriodDto> chiffreAffairesParMois,
        List<MatchPeriodDto> matchsParMois,
        List<TerrainStatisticsDto> tauxRemplissageParTerrain,
        List<MatchStatusStatisticsDto> repartitionMatchsParStatut,
        List<TerrainStatisticsDto> terrainsLesPlusUtilises,
        List<IncompleteMatchDto> prochainsMatchsIncomplets
) {
}
