package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.repositories.projections.MonthlyAmountProjection;
import be.ephec.pdw.padel.repositories.projections.SiteRevenueProjection;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
    long countByMatch(Match match);

    long countByMatchAndEstPayeeTrue(Match match);

    long countByMatchAndEstPayeeFalse(Match match);

    boolean existsByMatchAndMembre(Match match, Membre membre);

    boolean existsByMatchAndMembreAndEstPayeeFalse(Match match, Membre membre);

    Long id(Long id);

    List<Reservation> findByMembre(Membre membre);

    void deleteByMatchAndEstPayeeFalse(Match match);

    void deleteByMatchAndEstPayeeFalseAndIdNot(Match match, Long reservationId);

    @Query("""
            select coalesce(sum(r.montant), 0)
            from Reservation r
            where r.estPayee = true
              and r.match.dateHeureDebut >= :dateDebut
              and r.match.dateHeureDebut < :dateFin
              and (:siteId is null or r.match.terrain.site.id = :siteId)
              and (:terrainId is null or r.match.terrain.id = :terrainId)
            """)
    Double sumPaidRevenue(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") @Nullable Long siteId,
            @Param("terrainId") @Nullable Long terrainId
    );

    @Query("""
            select count(r)
            from Reservation r
            where r.statut = be.ephec.pdw.padel.model.StatutReservation.CONFIRMEE
              and r.match.dateHeureDebut >= :dateDebut
              and r.match.dateHeureDebut < :dateFin
              and (:siteId is null or r.match.terrain.site.id = :siteId)
              and (:terrainId is null or r.match.terrain.id = :terrainId)
            """)
    long countConfirmedReservations(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") @Nullable Long siteId,
            @Param("terrainId") @Nullable Long terrainId
    );

    @Query("""
            select count(r)
            from Reservation r
            where (r.estPayee = true or r.statut = be.ephec.pdw.padel.model.StatutReservation.CONFIRMEE)
              and r.match.dateHeureDebut >= :dateDebut
              and r.match.dateHeureDebut < :dateFin
              and (:siteId is null or r.match.terrain.site.id = :siteId)
              and (:terrainId is null or r.match.terrain.id = :terrainId)
            """)
    long countValidReservationsForFillRate(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") @Nullable Long siteId,
            @Param("terrainId") @Nullable Long terrainId
    );

    @Query("""
            select count(distinct r.membre.matricule)
            from Reservation r
            where (r.estPayee = true or r.statut = be.ephec.pdw.padel.model.StatutReservation.CONFIRMEE)
              and r.match.dateHeureDebut >= :dateDebut
              and r.match.dateHeureDebut < :dateFin
              and (:siteId is null or r.match.terrain.site.id = :siteId)
              and (:terrainId is null or r.match.terrain.id = :terrainId)
            """)
    long countActiveMembers(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") @Nullable Long siteId,
            @Param("terrainId") @Nullable Long terrainId
    );

    @Query("""
            select year(r.match.dateHeureDebut) as annee,
                   month(r.match.dateHeureDebut) as mois,
                   coalesce(sum(r.montant), 0) as montant
            from Reservation r
            where r.estPayee = true
              and r.match.dateHeureDebut >= :dateDebut
              and r.match.dateHeureDebut < :dateFin
              and (:siteId is null or r.match.terrain.site.id = :siteId)
              and (:terrainId is null or r.match.terrain.id = :terrainId)
            group by year(r.match.dateHeureDebut), month(r.match.dateHeureDebut)
            order by year(r.match.dateHeureDebut), month(r.match.dateHeureDebut)
            """)
    List<MonthlyAmountProjection> sumRevenueByMonth(
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("siteId") @Nullable Long siteId,
            @Param("terrainId") @Nullable Long terrainId
    );

    @Query("""
            select r.match.terrain.site.name as siteNom,
                   coalesce(sum(r.montant), 0) as montant
            from Reservation r
            where r.estPayee = true
              and (:siteId is null or r.match.terrain.site.id = :siteId)
            group by r.match.terrain.site.name
            order by r.match.terrain.site.name
            """)
    List<SiteRevenueProjection> sumRevenueBySite(@Param("siteId") @Nullable Long siteId);
}
