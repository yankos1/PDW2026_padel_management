package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.dto.AdminDashboardDto;
import be.ephec.pdw.padel.dto.DashboardSummaryDto;
import be.ephec.pdw.padel.dto.IncompleteMatchDto;
import be.ephec.pdw.padel.dto.MatchPeriodDto;
import be.ephec.pdw.padel.dto.MatchStatusStatisticsDto;
import be.ephec.pdw.padel.dto.RevenuePeriodDto;
import be.ephec.pdw.padel.dto.TerrainDTO;
import be.ephec.pdw.padel.dto.TerrainStatisticsDto;
import be.ephec.pdw.padel.exception.ForbiddenException;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreSite;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.repositories.projections.IncompleteMatchProjection;
import be.ephec.pdw.padel.repositories.projections.MonthlyAmountProjection;
import be.ephec.pdw.padel.repositories.projections.SiteRevenueProjection;
import be.ephec.pdw.padel.repositories.projections.TerrainStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private static final int PLACES_PAR_MATCH = 4;
    private static final int DUREE_MATCH_MINUTES = 90;
    private static final int PAUSE_ENTRE_MATCHS_MINUTES = 15;

    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final MembreRepository membreRepository;
    private final TerrainRepository terrainRepository;
    private final SiteRepository siteRepository;
    private final MatchService matchService;

    public long nombreMatchs(String matricule) {
        Membre admin = getAdmin(matricule);
        Long siteId = restrictedSiteId(admin, null);
        return matchRepository.countDashboardMatches(LocalDateTime.MIN, LocalDateTime.MAX, siteId, null);
    }

    public double chiffreAffaires(String matricule) {
        Membre admin = getAdmin(matricule);
        Long siteId = restrictedSiteId(admin, null);
        return zeroIfNull(reservationRepository.sumPaidRevenue(LocalDateTime.MIN, LocalDateTime.MAX, siteId, null)).doubleValue();
    }

    public long nombreMembres(String matricule) {
        Membre admin = getAdmin(matricule);
        Long siteId = restrictedSiteId(admin, null);
        return siteId == null ? membreRepository.countAllMembers() : membreRepository.countMembersBySite(siteId);
    }

    public long nombreTerrains(String matricule) {
        Membre admin = getAdmin(matricule);
        Long siteId = restrictedSiteId(admin, null);
        return terrainRepository.countAccessibleTerrains(siteId, null);
    }

    public long matchsComplets(String matricule) {
        Membre admin = getAdmin(matricule);
        Long siteId = restrictedSiteId(admin, null);
        return matchRepository.countMatchesByStatus(LocalDateTime.MIN, LocalDateTime.MAX, siteId, null).stream()
                .filter(stat -> stat.getStatut() != null && stat.getStatut().name().equals("COMPLET"))
                .mapToLong(stat -> defaultLong(stat.getNombre()))
                .sum();
    }

    public double tauxRemplissage(String matricule) {
        Membre admin = getAdmin(matricule);
        Long siteId = restrictedSiteId(admin, null);
        long matchs = matchRepository.countDashboardMatches(LocalDateTime.MIN, LocalDateTime.MAX, siteId, null);
        long reservations = reservationRepository.countValidReservationsForFillRate(LocalDateTime.MIN, LocalDateTime.MAX, siteId, null);
        return percentage(reservations, matchs * PLACES_PAR_MATCH).doubleValue();
    }

    public Map<String, Double> revenusParSite(String matricule) {
        Membre admin = getAdmin(matricule);
        Long siteId = restrictedSiteId(admin, null);
        return reservationRepository.sumRevenueBySite(siteId).stream()
                .collect(Collectors.toMap(
                        SiteRevenueProjection::getSiteNom,
                        projection -> zeroIfNull(projection.getMontant()).doubleValue()
                ));
    }

    public List<Site> sites(String matricule) {
        Membre admin = getAdmin(matricule);

        if (admin.getRole() == Role.ADMIN_GLOBAL) {
            return siteRepository.findAll();
        }

        if (admin instanceof MembreSite adminSite && adminSite.getSite() != null) {
            return List.of(adminSite.getSite());
        }

        return List.of();
    }

    public List<TerrainDTO> terrains(String matricule, Long siteId) {
        Membre admin = getAdmin(matricule);
        Long effectiveSiteId = restrictedSiteId(admin, siteId);

        return terrainRepository.findAccessibleTerrains(effectiveSiteId).stream()
                .map(terrain -> new TerrainDTO(
                        terrain.getId(),
                        terrain.getNom(),
                        terrain.getSite().getHeureOuverture().toString(),
                        terrain.getSite().getHeureFermeture().toString()
                ))
                .toList();
    }

    @Transactional
    public AdminDashboardDto dashboard(String matricule, LocalDate dateDebut, LocalDate dateFin, Long siteId, Long terrainId) {
        Membre admin = getAdmin(matricule);
        LocalDate startDate = dateDebut != null ? dateDebut : YearMonth.now().atDay(1);
        LocalDate endDate = dateFin != null ? dateFin : LocalDate.now();
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La date de fin doit etre posterieure ou egale a la date de debut");
        }

        Long effectiveSiteId = restrictedSiteId(admin, siteId);
        validateTerrainAccess(effectiveSiteId, terrainId);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        matchService.synchroniserStatutsPourDashboard(start, endExclusive, effectiveSiteId, terrainId);
        long nombreMatchs = matchRepository.countDashboardMatches(start, endExclusive, effectiveSiteId, terrainId);
        long reservationsConfirmees = reservationRepository.countConfirmedReservations(start, endExclusive, effectiveSiteId, terrainId);
        long reservationsValides = reservationRepository.countValidReservationsForFillRate(start, endExclusive, effectiveSiteId, terrainId);
        long matchsAnnules = matchRepository.countCancelledMatches(start, endExclusive, effectiveSiteId, terrainId);
        List<IncompleteMatchProjection> prochainsIncomplets = matchRepository.upcomingIncompleteMatches(LocalDateTime.now(), effectiveSiteId, terrainId);
        long creneauxUtilises = matchRepository.countUsedSlots(start, endExclusive, effectiveSiteId, terrainId);
        long creneauxDisponibles = creneauxDisponibles(startDate, endDate, effectiveSiteId, terrainId);

        DashboardSummaryDto summary = new DashboardSummaryDto(
                zeroIfNull(reservationRepository.sumPaidRevenue(start, endExclusive, effectiveSiteId, terrainId)),
                nombreMatchs,
                reservationsConfirmees,
                percentage(reservationsValides, nombreMatchs * PLACES_PAR_MATCH),
                percentage(creneauxUtilises, creneauxDisponibles),
                soldesDus(effectiveSiteId),
                reservationRepository.countActiveMembers(start, endExclusive, effectiveSiteId, terrainId),
                matchsAnnules,
                prochainsIncomplets.size()
        );

        List<TerrainStatisticsDto> terrainStats = matchRepository.terrainStatistics(start, endExclusive, effectiveSiteId, terrainId).stream()
                .map(this::toTerrainStatistics)
                .toList();

        return new AdminDashboardDto(
                summary,
                reservationRepository.sumRevenueByMonth(start, endExclusive, effectiveSiteId, terrainId).stream()
                        .map(this::toRevenuePeriod)
                        .toList(),
                matchRepository.countMatchesByMonth(start, endExclusive, effectiveSiteId, terrainId).stream()
                        .map(matchs -> new MatchPeriodDto(matchs.getAnnee(), matchs.getMois(), defaultLong(matchs.getNombre())))
                        .toList(),
                terrainStats,
                matchRepository.countMatchesByStatus(start, endExclusive, effectiveSiteId, terrainId).stream()
                        .map(stat -> new MatchStatusStatisticsDto(stat.getStatut(), defaultLong(stat.getNombre())))
                        .toList(),
                terrainStats,
                prochainsIncomplets.stream()
                        .map(this::toIncompleteMatch)
                        .toList()
        );
    }

    private Membre getAdmin(String matricule) {
        Membre membre = membreRepository.findById(matricule)
                .orElseThrow(() -> new ForbiddenException("Utilisateur inconnu"));

        if (membre.getRole() != Role.ADMIN_GLOBAL && membre.getRole() != Role.ADMIN_SITE) {
            throw new ForbiddenException("Acces refuse");
        }

        return membre;
    }

    private Long restrictedSiteId(Membre admin, Long requestedSiteId) {
        if (admin.getRole() == Role.ADMIN_GLOBAL) {
            return requestedSiteId;
        }

        if (admin instanceof MembreSite adminSite && adminSite.getSite() != null) {
            Long adminSiteId = adminSite.getSite().getId();
            if (requestedSiteId != null && !Objects.equals(requestedSiteId, adminSiteId)) {
                throw new ForbiddenException("Acces refuse");
            }
            return adminSiteId;
        }

        throw new ForbiddenException("Acces refuse");
    }

    private void validateTerrainAccess(Long siteId, Long terrainId) {
        if (terrainId == null) {
            return;
        }

        Terrain terrain = terrainRepository.findById(terrainId)
                .orElseThrow(() -> new ForbiddenException("Terrain inaccessible"));

        if (siteId != null && (terrain.getSite() == null || !Objects.equals(terrain.getSite().getId(), siteId))) {
            throw new ForbiddenException("Acces refuse");
        }
    }

    private BigDecimal soldesDus(Long siteId) {
        Double value = siteId == null ? membreRepository.sumAllSoldesDus() : membreRepository.sumSoldesDusBySite(siteId);
        return zeroIfNull(value);
    }

    private long creneauxDisponibles(LocalDate dateDebut, LocalDate dateFin, Long siteId, Long terrainId) {
        return siteRepository.findSitesForDashboard(siteId).stream()
                .mapToLong(site -> creneauxDisponiblesPourSite(site, dateDebut, dateFin, terrainId))
                .sum();
    }

    private long creneauxDisponiblesPourSite(Site site, LocalDate dateDebut, LocalDate dateFin, Long terrainId) {
        long terrains = terrainId == null
                ? site.getTerrains().size()
                : site.getTerrains().stream().filter(terrain -> Objects.equals(terrain.getId(), terrainId)).count();
        if (terrains == 0 || site.getHeureOuverture() == null || site.getHeureFermeture() == null) {
            return 0;
        }

        long slotsParJour = slotsParTerrainParJour(site.getHeureOuverture(), site.getHeureFermeture());
        if (slotsParJour == 0) {
            return 0;
        }

        long joursOuverts = dateDebut.datesUntil(dateFin.plusDays(1))
                .filter(date -> site.getJourFermeture() == null || site.getJourFermeture().stream().noneMatch(jour -> Objects.equals(jour.getDate(), date)))
                .count();

        return joursOuverts * slotsParJour * terrains;
    }

    private long slotsParTerrainParJour(LocalTime ouverture, LocalTime fermeture) {
        long minutesOuverture = Duration.between(ouverture, fermeture).toMinutes();
        if (minutesOuverture < DUREE_MATCH_MINUTES) {
            return 0;
        }

        return ((minutesOuverture - DUREE_MATCH_MINUTES) / (DUREE_MATCH_MINUTES + PAUSE_ENTRE_MATCHS_MINUTES)) + 1;
    }

    private TerrainStatisticsDto toTerrainStatistics(TerrainStatsProjection projection) {
        long nombreMatchs = defaultLong(projection.getNombreMatchs());
        long reservationsValides = defaultLong(projection.getReservationsValides());
        return new TerrainStatisticsDto(
                projection.getTerrainId(),
                projection.getTerrainNom(),
                projection.getSiteId(),
                projection.getSiteNom(),
                nombreMatchs,
                reservationsValides,
                percentage(reservationsValides, nombreMatchs * PLACES_PAR_MATCH)
        );
    }

    private RevenuePeriodDto toRevenuePeriod(MonthlyAmountProjection projection) {
        return new RevenuePeriodDto(
                projection.getAnnee(),
                projection.getMois(),
                zeroIfNull(projection.getMontant())
        );
    }

    private IncompleteMatchDto toIncompleteMatch(IncompleteMatchProjection projection) {
        long participants = defaultLong(projection.getParticipants());
        return new IncompleteMatchDto(
                projection.getMatchId(),
                projection.getDateHeureDebut(),
                projection.getTerrainId(),
                projection.getTerrainNom(),
                projection.getSiteId(),
                projection.getSiteNom(),
                participants,
                Math.max(0, PLACES_PAR_MATCH - participants)
        );
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(Double value) {
        return BigDecimal.valueOf(value == null ? 0 : value).setScale(2, RoundingMode.HALF_UP);
    }

    private long defaultLong(Long value) {
        return value == null ? 0 : value;
    }
}
