package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.StatutMatch;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.projections.IncompleteMatchProjection;
import be.ephec.pdw.padel.repositories.projections.MatchStatusStatsProjection;
import be.ephec.pdw.padel.repositories.projections.MonthlyCountProjection;
import be.ephec.pdw.padel.repositories.projections.TerrainStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match,Long> {


    List<Match> findByTerrain(Terrain terrain);

    List<Match> findByDateHeureDebut(LocalDateTime dateHeureDebut);

    List<Match> findByStatut(StatutMatch statut);

    List<Match> findByStatutAndEstPublic(StatutMatch statut, boolean estPublic);

    boolean existsByTerrainAndDateHeureDebut(Terrain terrain, LocalDateTime dateHeureDebut);

    long countByStatut(StatutMatch statutMatch);

    @Query("""
            select count(m)
            from Match m
            where m.dateHeureDebut >= :dateDebut
              and m.dateHeureDebut < :dateFin
              and (:siteId is null or m.terrain.site.id = :siteId)
              and (:terrainId is null or m.terrain.id = :terrainId)
            """)
    long countDashboardMatches(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") Long siteId,
            @Param("terrainId") Long terrainId
    );

    @Query("""
            select count(m)
            from Match m
            where m.dateHeureDebut >= :dateDebut
              and m.dateHeureDebut < :dateFin
              and m.statut = be.ephec.pdw.padel.model.StatutMatch.ANNULE
              and (:siteId is null or m.terrain.site.id = :siteId)
              and (:terrainId is null or m.terrain.id = :terrainId)
            """)
    long countCancelledMatches(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") Long siteId,
            @Param("terrainId") Long terrainId
    );

    @Query("""
            select count(m)
            from Match m
            where m.dateHeureDebut >= :dateDebut
              and m.dateHeureDebut < :dateFin
              and m.statut <> be.ephec.pdw.padel.model.StatutMatch.ANNULE
              and (:siteId is null or m.terrain.site.id = :siteId)
              and (:terrainId is null or m.terrain.id = :terrainId)
            """)
    long countUsedSlots(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") Long siteId,
            @Param("terrainId") Long terrainId
    );

    @Query("""
            select year(m.dateHeureDebut) as annee,
                   month(m.dateHeureDebut) as mois,
                   count(m) as nombre
            from Match m
            where m.dateHeureDebut >= :dateDebut
              and m.dateHeureDebut < :dateFin
              and (:siteId is null or m.terrain.site.id = :siteId)
              and (:terrainId is null or m.terrain.id = :terrainId)
            group by year(m.dateHeureDebut), month(m.dateHeureDebut)
            order by year(m.dateHeureDebut), month(m.dateHeureDebut)
            """)
    List<MonthlyCountProjection> countMatchesByMonth(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") Long siteId,
            @Param("terrainId") Long terrainId
    );

    @Query("""
            select m.statut as statut, count(m) as nombre
            from Match m
            where m.dateHeureDebut >= :dateDebut
              and m.dateHeureDebut < :dateFin
              and (:siteId is null or m.terrain.site.id = :siteId)
              and (:terrainId is null or m.terrain.id = :terrainId)
            group by m.statut
            order by m.statut
            """)
    List<MatchStatusStatsProjection> countMatchesByStatus(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") Long siteId,
            @Param("terrainId") Long terrainId
    );

    @Query("""
            select t.id as terrainId,
                   t.nom as terrainNom,
                   s.id as siteId,
                   s.name as siteNom,
                   count(distinct m.id) as nombreMatchs,
                   sum(case when r.estPayee = true or r.statut = be.ephec.pdw.padel.model.StatutReservation.CONFIRMEE then 1 else 0 end) as reservationsValides
            from Match m
            join m.terrain t
            join t.site s
            left join m.reservations r
            where m.dateHeureDebut >= :dateDebut
              and m.dateHeureDebut < :dateFin
              and (:siteId is null or s.id = :siteId)
              and (:terrainId is null or t.id = :terrainId)
            group by t.id, t.nom, s.id, s.name
            order by count(distinct m.id) desc, t.nom
            """)
    List<TerrainStatsProjection> terrainStatistics(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") Long siteId,
            @Param("terrainId") Long terrainId
    );

    @Query("""
            select m.id as matchId,
                   m.dateHeureDebut as dateHeureDebut,
                   t.id as terrainId,
                   t.nom as terrainNom,
                   s.id as siteId,
                   s.name as siteNom,
                   sum(case when r.estPayee = true or r.statut = be.ephec.pdw.padel.model.StatutReservation.CONFIRMEE then 1 else 0 end) as participants
            from Match m
            join m.terrain t
            join t.site s
            left join m.reservations r
            where m.dateHeureDebut >= :maintenant
              and m.statut <> be.ephec.pdw.padel.model.StatutMatch.ANNULE
              and (:siteId is null or s.id = :siteId)
              and (:terrainId is null or t.id = :terrainId)
            group by m.id, m.dateHeureDebut, t.id, t.nom, s.id, s.name
            having sum(case when r.estPayee = true or r.statut = be.ephec.pdw.padel.model.StatutReservation.CONFIRMEE then 1 else 0 end) < 4
            order by m.dateHeureDebut
            """)
    List<IncompleteMatchProjection> upcomingIncompleteMatches(
            @Param("maintenant") LocalDateTime maintenant,
            @Param("siteId") Long siteId,
            @Param("terrainId") Long terrainId
    );
}
