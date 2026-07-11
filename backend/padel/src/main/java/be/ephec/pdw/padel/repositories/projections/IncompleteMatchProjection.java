package be.ephec.pdw.padel.repositories.projections;

import java.time.LocalDateTime;

public interface IncompleteMatchProjection {
    Long getMatchId();
    LocalDateTime getDateHeureDebut();
    Long getTerrainId();
    String getTerrainNom();
    Long getSiteId();
    String getSiteNom();
    Long getParticipants();
}
