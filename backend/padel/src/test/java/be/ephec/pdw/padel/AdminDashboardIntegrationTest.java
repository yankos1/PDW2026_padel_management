package be.ephec.pdw.padel;

import be.ephec.pdw.padel.dto.AdminDashboardDto;
import be.ephec.pdw.padel.dto.MatchStatusStatisticsDto;
import be.ephec.pdw.padel.exception.ForbiddenException;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreSite;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.model.StatutMatch;
import be.ephec.pdw.padel.model.StatutReservation;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.service.AdminService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class AdminDashboardIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private TerrainRepository terrainRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private EntityManager entityManager;

    private Membre organisateur;
    private Terrain terrain;
    private int memberSequence;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        matchRepository.deleteAll();
        terrainRepository.deleteAll();
        membreRepository.deleteAll();
        siteRepository.deleteAll();

        Site site = siteRepository.saveAndFlush(site("Site dashboard"));
        terrain = terrainRepository.saveAndFlush(dashboardTerrain(site));
        organisateur = membreRepository.saveAndFlush(membre("G8000", Role.USER));
        membreRepository.saveAndFlush(globalAdmin());
        entityManager.clear();
    }

    @Test
    void dashboardShouldPersistSynchronizedStatusesBeforeAggregating() {
        Match pastPlanned = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().minusHours(3), terrain);

        AdminDashboardDto dashboard = adminService.dashboard("G8001", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), null, null);

        entityManager.clear();

        assertEquals(StatutMatch.TERMINE, reloadedStatus(pastPlanned.getId()));
        Map<StatutMatch, Long> repartition = statusRepartition(dashboard);
        assertEquals(1L, repartition.get(StatutMatch.TERMINE));
        assertEquals(0L, repartition.getOrDefault(StatutMatch.PLANIFIE, 0L));
    }

    @Test
    void dashboardShouldReturnCorrectDistributionForAllStatuses() {
        Match planned = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().plusDays(2), terrain);
        Match complete = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().plusDays(3), terrain);
        saveFourPaidReservations(complete);
        Match finished = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().minusHours(3), terrain);
        Match cancelled = saveMatch(StatutMatch.ANNULE, LocalDateTime.now().minusHours(2), terrain);

        AdminDashboardDto dashboard = adminService.dashboard("G8001", LocalDate.now().minusDays(1), LocalDate.now().plusDays(4), null, null);

        entityManager.clear();

        assertEquals(StatutMatch.PLANIFIE, reloadedStatus(planned.getId()));
        assertEquals(StatutMatch.COMPLET, reloadedStatus(complete.getId()));
        assertEquals(StatutMatch.TERMINE, reloadedStatus(finished.getId()));
        assertEquals(StatutMatch.ANNULE, reloadedStatus(cancelled.getId()));

        Map<StatutMatch, Long> repartition = statusRepartition(dashboard);
        assertEquals(1L, repartition.get(StatutMatch.PLANIFIE));
        assertEquals(1L, repartition.get(StatutMatch.COMPLET));
        assertEquals(1L, repartition.get(StatutMatch.TERMINE));
        assertEquals(1L, repartition.get(StatutMatch.ANNULE));
    }

    @Test
    void siteAdminShouldNotRequestDashboardForAnotherSite() {
        Site adminSite = siteRepository.saveAndFlush(site("Site admin"));
        Site otherSite = siteRepository.saveAndFlush(site("Autre site"));
        membreRepository.saveAndFlush(siteAdmin(adminSite));
        entityManager.clear();

        assertThrows(ForbiddenException.class, () ->
                adminService.dashboard("S8001", LocalDate.now(), LocalDate.now().plusDays(1), otherSite.getId(), null)
        );
    }

    private Map<StatutMatch, Long> statusRepartition(AdminDashboardDto dashboard) {
        return dashboard.repartitionMatchsParStatut().stream()
                .collect(Collectors.toMap(
                        MatchStatusStatisticsDto::statut,
                        MatchStatusStatisticsDto::nombre
                ));
    }

    private StatutMatch reloadedStatus(Long matchId) {
        StatutMatch statut = matchRepository.findById(matchId).orElseThrow().getStatut();
        entityManager.clear();
        return statut;
    }

    private Match saveMatch(StatutMatch statut, LocalDateTime dateHeureDebut, Terrain terrain) {
        Match match = Match.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .dateHeureDebut(dateHeureDebut)
                .estPublic(true)
                .statut(statut)
                .build();
        Match saved = matchRepository.saveAndFlush(match);
        entityManager.clear();
        return saved;
    }

    private void saveFourPaidReservations(Match match) {
        for (int i = 0; i < 4; i++) {
            reservationRepository.saveAndFlush(paidReservation(match, nextMember()));
        }
        entityManager.clear();
    }

    private Reservation paidReservation(Match match, Membre membre) {
        Membre savedMember = membreRepository.saveAndFlush(membre);
        return Reservation.builder()
                .match(matchRepository.findById(match.getId()).orElseThrow())
                .membre(savedMember)
                .dateReservation(LocalDateTime.now())
                .datePaiement(LocalDateTime.now())
                .montant(15.0)
                .estPayee(true)
                .statut(StatutReservation.CONFIRMEE)
                .build();
    }

    private MembreGlobal nextMember() {
        memberSequence++;
        return membre("G81" + memberSequence, Role.USER);
    }

    private Site site(String name) {
        return Site.builder()
                .name(name)
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .build();
    }

    private Terrain dashboardTerrain(Site site) {
        return Terrain.builder()
                .nom("Terrain dashboard")
                .site(site)
                .build();
    }

    private MembreGlobal globalAdmin() {
        return membre("G8001", Role.ADMIN_GLOBAL);
    }

    private MembreGlobal membre(String matricule, Role role) {
        MembreGlobal membre = new MembreGlobal();
        membre.setMatricule(matricule);
        membre.setNom("Nom");
        membre.setPrenom("Prenom");
        membre.setEmail(matricule + "@example.test");
        membre.setRole(role);
        return membre;
    }

    private MembreSite siteAdmin(Site site) {
        MembreSite membre = new MembreSite();
        membre.setMatricule("S8001");
        membre.setNom("Admin");
        membre.setPrenom("Site");
        membre.setEmail("S8001@example.test");
        membre.setRole(Role.ADMIN_SITE);
        membre.setSite(site);
        return membre;
    }
}
