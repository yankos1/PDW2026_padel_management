package be.ephec.pdw.padel.repositories.projections;

import be.ephec.pdw.padel.model.StatutMatch;

public interface MatchStatusStatsProjection {
    StatutMatch getStatut();
    Long getNombre();
}
