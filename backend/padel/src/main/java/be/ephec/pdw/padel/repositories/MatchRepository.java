package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.StatutMatch;
import be.ephec.pdw.padel.model.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match,Long> {


    List<Match> findByTerrain(Terrain terrain);

    List<Match> findByDateHeureDebut(LocalDateTime dateHeureDebut);

    List<Match> findByStatutAndEstPublic(StatutMatch statut, boolean estPublic);

    boolean existsByTerrainAndDateHeureDebut(Terrain terrain, LocalDateTime dateHeureDebut);

    long countByStatut(StatutMatch statutMatch);
}
