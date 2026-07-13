package be.ephec.pdw.padel;

import be.ephec.pdw.padel.dto.TerrainDTO;
import be.ephec.pdw.padel.exception.BusinessRuleException;
import be.ephec.pdw.padel.model.JourFermeture;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.Role;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.JourFermetureRepository;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.service.MatchService;
import be.ephec.pdw.padel.service.TerrainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class JourFermetureIntegrationTest {

    @Autowired private TerrainService terrainService;
    @Autowired private MatchService matchService;
    @Autowired private JourFermetureRepository jourFermetureRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private MembreRepository membreRepository;
    @Autowired private TerrainRepository terrainRepository;
    @Autowired private SiteRepository siteRepository;

    private Site siteA;
    private Site siteB;
    private Terrain terrainA;
    private Terrain terrainB;
    private MembreGlobal organisateur;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        matchRepository.deleteAll();
        jourFermetureRepository.deleteAll();
        terrainRepository.deleteAll();
        membreRepository.deleteAll();
        siteRepository.deleteAll();

        siteA = siteRepository.saveAndFlush(site("Site A"));
        siteB = siteRepository.saveAndFlush(site("Site B"));
        terrainA = terrainRepository.saveAndFlush(terrain("Terrain A", siteA));
        terrainB = terrainRepository.saveAndFlush(terrain("Terrain B", siteB));
        organisateur = membreRepository.saveAndFlush(membreGlobal("G6000"));
        date = LocalDate.now().plusDays(2);
    }

    @Test
    void globalClosureShouldCloseAllSites() {
        saveClosure(null, date);

        assertTrue(terrainService.estSiteFerme(siteA, date));
        assertTrue(terrainService.estSiteFerme(siteB, date));
    }

    @Test
    void localClosureShouldCloseItsSite() {
        saveClosure(siteA, date);

        assertTrue(terrainService.estSiteFerme(siteA, date));
    }

    @Test
    void localClosureShouldNotCloseAnotherSite() {
        saveClosure(siteA, date);

        assertFalse(terrainService.estSiteFerme(siteB, date));
        assertEquals(List.of(terrainB.getId()), availableTerrainIds(date));
        assertTrue(terrainService.getTerrainsDisponiblesParCreneau(date, "10:00", siteA.getId()).isEmpty());
        assertEquals(List.of(terrainB.getId()), terrainService.getTerrainsDisponiblesParCreneau(date, "10:00", siteB.getId())
                .stream().map(TerrainDTO::id).toList());
        assertFalse(terrainService.getCreneauxDisponibles(date).isEmpty());
    }

    @Test
    void dateWithoutClosureShouldLeaveSitesOpen() {
        assertFalse(terrainService.estSiteFerme(siteA, date));
        assertFalse(terrainService.estSiteFerme(siteB, date));
        assertEquals(Set.of(terrainA.getId(), terrainB.getId()), Set.copyOf(availableTerrainIds(date)));
    }

    @Test
    void shouldRejectMatchCreationDuringApplicableClosure() {
        saveClosure(siteA, date);

        assertThrows(BusinessRuleException.class, () -> matchService.creerMatch(
                organisateur.getMatricule(),
                terrainA.getId(),
                date.atTime(10, 0),
                true
        ));
        assertEquals(0, matchRepository.count());
        assertEquals(0, reservationRepository.count());
    }

    @Test
    void applicableClosureShouldExposeNoAvailableTerrainOrSlot() {
        saveClosure(null, date);

        assertTrue(terrainService.getTerrainsDisponibles(date).isEmpty());
        assertTrue(terrainService.getTerrainsDisponiblesParCreneau(date, "10:00", siteA.getId()).isEmpty());
        assertTrue(terrainService.getCreneauxDisponibles(date).isEmpty());
    }

    private List<Long> availableTerrainIds(LocalDate targetDate) {
        return terrainService.getTerrainsDisponibles(targetDate).stream()
                .map(TerrainDTO::id)
                .toList();
    }

    private void saveClosure(Site site, LocalDate targetDate) {
        jourFermetureRepository.saveAndFlush(JourFermeture.builder()
                .site(site)
                .date(targetDate)
                .build());
    }

    private Site site(String name) {
        return Site.builder()
                .name(name)
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .build();
    }

    private Terrain terrain(String name, Site site) {
        return Terrain.builder()
                .nom(name)
                .site(site)
                .build();
    }

    private MembreGlobal membreGlobal(String matricule) {
        MembreGlobal membre = new MembreGlobal();
        membre.setMatricule(matricule);
        membre.setNom("Nom");
        membre.setPrenom("Prenom");
        membre.setEmail(matricule + "@example.test");
        membre.setRole(Role.USER);
        return membre;
    }
}
