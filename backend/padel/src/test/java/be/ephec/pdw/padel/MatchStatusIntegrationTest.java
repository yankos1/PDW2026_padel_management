package be.ephec.pdw.padel;

import be.ephec.pdw.padel.dto.MatchReponseDTO;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreGlobal;
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
import be.ephec.pdw.padel.service.MatchService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
class MatchStatusIntegrationTest {

    @Autowired
    private MatchService matchService;

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

    private Terrain terrain;
    private Membre organisateur;
    private int memberSequence;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        matchRepository.deleteAll();
        terrainRepository.deleteAll();
        siteRepository.deleteAll();
        membreRepository.deleteAll();

        Site site = siteRepository.saveAndFlush(statusSite());
        terrain = terrainRepository.saveAndFlush(statusTerrain(site));
        organisateur = membreRepository.saveAndFlush(user("G9000"));
        entityManager.clear();
    }

    @Test
    void pastPlannedMatchShouldBecomeFinished() {
        Match match = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().minusHours(3));

        synchronize(match.getId());

        assertStatus(match.getId(), StatutMatch.TERMINE);
    }

    @Test
    void pastCompleteMatchShouldBecomeFinished() {
        Match match = saveMatch(StatutMatch.COMPLET, LocalDateTime.now().minusHours(3));
        savePaidReservations(match, 4);

        synchronize(match.getId());

        assertStatus(match.getId(), StatutMatch.TERMINE);
    }

    @Test
    void pastCancelledMatchShouldRemainCancelled() {
        Match match = saveMatch(StatutMatch.ANNULE, LocalDateTime.now().minusHours(3));

        synchronize(match.getId());

        assertStatus(match.getId(), StatutMatch.ANNULE);
    }

    @Test
    void futureIncompleteMatchShouldRemainPlanned() {
        Match match = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().plusDays(2));

        synchronize(match.getId());

        assertStatus(match.getId(), StatutMatch.PLANIFIE);
    }

    @Test
    void futureMatchWithThreePaidReservationsShouldRemainPlanned() {
        Match match = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().plusDays(2));
        savePaidReservations(match, 3);

        synchronize(match.getId());

        assertStatus(match.getId(), StatutMatch.PLANIFIE);
    }

    @Test
    void futureMatchWithFourPaidReservationsShouldBecomeComplete() {
        Match match = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().plusDays(2));
        savePaidReservations(match, 4);

        synchronize(match.getId());

        assertStatus(match.getId(), StatutMatch.COMPLET);
    }

    @Test
    void confirmedUnpaidReservationsShouldNotCompletePublicMatch() {
        Match match = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().plusDays(2));
        saveFourConfirmedUnpaidReservations(match);

        synchronize(match.getId());

        assertStatus(match.getId(), StatutMatch.PLANIFIE);
    }

    @Test
    void availableMatchesShouldExcludeCompleteFinishedAndCancelledMatches() {
        Match available = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().plusDays(2));
        savePaidReservations(available, 1);
        Match complete = saveMatch(StatutMatch.PLANIFIE, LocalDateTime.now().plusDays(3));
        savePaidReservations(complete, 4);
        Match finished = saveMatch(StatutMatch.TERMINE, LocalDateTime.now().minusHours(3));
        Match cancelled = saveMatch(StatutMatch.ANNULE, LocalDateTime.now().plusDays(4));
        entityManager.clear();

        List<MatchReponseDTO> disponibles = matchService.matchsDisponibles();

        assertEquals(List.of(available.getId()), disponibles.stream().map(MatchReponseDTO::id).toList());
        assertStatus(available.getId(), StatutMatch.PLANIFIE);
        assertStatus(complete.getId(), StatutMatch.COMPLET);
        assertStatus(finished.getId(), StatutMatch.TERMINE);
        assertStatus(cancelled.getId(), StatutMatch.ANNULE);
    }

    private void synchronize(Long matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        matchService.synchroniserStatut(match);
        entityManager.clear();
    }

    private void assertStatus(Long matchId, StatutMatch expected) {
        Match reloaded = matchRepository.findById(matchId).orElseThrow();
        assertEquals(expected, reloaded.getStatut());
        entityManager.clear();
    }

    private Match saveMatch(StatutMatch statut, LocalDateTime dateHeureDebut) {
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

    private void savePaidReservations(Match match, int count) {
        for (int i = 0; i < count; i++) {
            reservationRepository.saveAndFlush(reservation(match, nextMember(), true));
        }
        entityManager.clear();
    }

    private void saveFourConfirmedUnpaidReservations(Match match) {
        for (int i = 0; i < 4; i++) {
            reservationRepository.saveAndFlush(reservation(match, nextMember(), false));
        }
        entityManager.clear();
    }

    private Reservation reservation(Match match, Membre membre, boolean estPayee) {
        Membre savedMember = membreRepository.saveAndFlush(membre);
        return Reservation.builder()
                .match(matchRepository.findById(match.getId()).orElseThrow())
                .membre(savedMember)
                .dateReservation(LocalDateTime.now())
                .datePaiement(estPayee ? LocalDateTime.now() : null)
                .montant(estPayee ? 15.0 : 0.0)
                .estPayee(estPayee)
                .statut(StatutReservation.CONFIRMEE)
                .build();
    }

    private MembreGlobal nextMember() {
        memberSequence++;
        return user("G91" + memberSequence);
    }

    private Site statusSite() {
        return Site.builder()
                .name("Site statuts")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .build();
    }

    private Terrain statusTerrain(Site site) {
        return Terrain.builder()
                .nom("Terrain statuts")
                .site(site)
                .build();
    }

    private MembreGlobal user(String matricule) {
        MembreGlobal membre = new MembreGlobal();
        membre.setMatricule(matricule);
        membre.setNom("Nom");
        membre.setPrenom("Prenom");
        membre.setEmail(matricule + "@example.test");
        membre.setRole(Role.USER);
        assertFalse(membre.aUnePenaliteActive());
        return membre;
    }
}
