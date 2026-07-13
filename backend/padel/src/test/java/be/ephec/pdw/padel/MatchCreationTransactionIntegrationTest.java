package be.ephec.pdw.padel;

import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.model.StatutReservation;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.JourFermetureRepository;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class MatchCreationTransactionIntegrationTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchRepository matchRepository;

    @MockitoSpyBean
    private ReservationRepository reservationRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private TerrainRepository terrainRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private JourFermetureRepository jourFermetureRepository;

    private MembreGlobal organisateur;
    private Terrain terrain;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        matchRepository.deleteAll();
        jourFermetureRepository.deleteAll();
        terrainRepository.deleteAll();
        siteRepository.deleteAll();
        membreRepository.deleteAll();

        Site site = siteRepository.saveAndFlush(Site.builder()
                .name("Site transaction")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .build());
        terrain = terrainRepository.saveAndFlush(Terrain.builder()
                .nom("Terrain transaction")
                .site(site)
                .build());

        organisateur = new MembreGlobal();
        organisateur.setMatricule("G7000");
        organisateur.setNom("Organisateur");
        organisateur.setPrenom("Transaction");
        organisateur.setEmail("G7000@example.test");
        organisateur.setRole(Role.USER);
        organisateur = membreRepository.saveAndFlush(organisateur);
    }

    @Test
    void shouldPersistMatchAndOrganizerReservationTogether() {
        Match match = matchService.creerMatch(
                organisateur.getMatricule(),
                terrain.getId(),
                LocalDateTime.now().plusDays(2),
                true
        );

        assertEquals(1, matchRepository.count());
        assertEquals(1, reservationRepository.count());

        Reservation reservation = reservationRepository.findAll().getFirst();
        assertEquals(match.getId(), reservation.getMatch().getId());
        assertEquals(organisateur.getMatricule(), reservation.getMembre().getMatricule());
        assertSame(StatutReservation.CONFIRMEE, reservation.getStatut());
    }

    @Test
    void shouldRollbackMatchWhenOrganizerReservationPersistenceFails() {
        doThrow(new DataAccessResourceFailureException("Echec technique simule"))
                .when(reservationRepository).save(any(Reservation.class));

        assertThrows(DataAccessResourceFailureException.class, () ->
                matchService.creerMatch(
                        organisateur.getMatricule(),
                        terrain.getId(),
                        LocalDateTime.now().plusDays(2),
                        false
                )
        );

        assertEquals(0, matchRepository.count());
        assertEquals(0, reservationRepository.count());
    }
}
