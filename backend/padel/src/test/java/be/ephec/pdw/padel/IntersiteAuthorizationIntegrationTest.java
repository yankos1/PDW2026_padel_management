package be.ephec.pdw.padel;

import be.ephec.pdw.padel.model.JourFermeture;
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
import be.ephec.pdw.padel.repositories.JourFermetureRepository;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntersiteAuthorizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private JourFermetureRepository jourFermetureRepository;
    @Autowired private TerrainRepository terrainRepository;
    @Autowired private MembreRepository membreRepository;
    @Autowired private SiteRepository siteRepository;

    private MembreGlobal adminGlobal;
    private MembreSite adminSiteA;
    private MembreGlobal utilisateurNormal;
    private MembreGlobal membreSiteA;
    private MembreGlobal membreSiteB;
    private Match matchA;
    private Match matchB;
    private Reservation reservationA;
    private Reservation reservationB;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        matchRepository.deleteAll();
        jourFermetureRepository.deleteAll();
        terrainRepository.deleteAll();
        membreRepository.deleteAll();
        siteRepository.deleteAll();

        Site siteA = siteRepository.saveAndFlush(site("Site A"));
        Site siteB = siteRepository.saveAndFlush(site("Site B"));
        Terrain terrainA = terrainRepository.saveAndFlush(terrain("Terrain A", siteA));
        Terrain terrainB = terrainRepository.saveAndFlush(terrain("Terrain B", siteB));

        adminGlobal = membreRepository.saveAndFlush(global("G5000", Role.ADMIN_GLOBAL));
        adminSiteA = membreRepository.saveAndFlush(siteMember("S5000", Role.ADMIN_SITE, siteA));
        utilisateurNormal = membreRepository.saveAndFlush(global("G5001", Role.USER));
        membreSiteA = membreRepository.saveAndFlush(global("G5002", Role.USER));
        membreSiteB = membreRepository.saveAndFlush(global("G5003", Role.USER));

        matchA = matchRepository.saveAndFlush(match(terrainA, membreSiteA, 2));
        matchB = matchRepository.saveAndFlush(match(terrainB, membreSiteB, 3));
        reservationA = reservationRepository.saveAndFlush(reservation(matchA, membreSiteA));
        reservationB = reservationRepository.saveAndFlush(reservation(matchB, membreSiteB));
    }

    @Test
    void globalAdminCanReadReservationsFromAnySite() throws Exception {
        mockMvc.perform(get("/reservation/membre/{matricule}", membreSiteB.getMatricule())
                        .header("Authorization", bearer(adminGlobal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reservationB.getId()));
    }

    @Test
    void siteAdminCanReadReservationsFromOwnSite() throws Exception {
        mockMvc.perform(get("/reservation/membre/{matricule}", membreSiteA.getMatricule())
                        .header("Authorization", bearer(adminSiteA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reservationA.getId()));
    }

    @Test
    void siteAdminCannotReadReservationsFromAnotherSite() throws Exception {
        mockMvc.perform(get("/reservation/membre/{matricule}", membreSiteB.getMatricule())
                        .header("Authorization", bearer(adminSiteA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acces refuse"));
    }

    @Test
    void siteAdminCannotPayReservationFromAnotherSite() throws Exception {
        mockMvc.perform(put("/reservation/{id}/payer", reservationB.getId())
                        .header("Authorization", bearer(adminSiteA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acces refuse"));

        assertFalse(reservationRepository.findById(reservationB.getId()).orElseThrow().isEstPayee());
    }

    @Test
    void normalUserCannotReadAnotherMembersReservations() throws Exception {
        mockMvc.perform(get("/reservation/membre/{matricule}", membreSiteB.getMatricule())
                        .header("Authorization", bearer(utilisateurNormal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acces refuse"));
    }

    @Test
    void siteAdminStatisticsRemainRestrictedToOwnSite() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .param("dateDebut", LocalDate.now().toString())
                        .param("dateFin", LocalDate.now().plusDays(4).toString())
                        .header("Authorization", bearer(adminSiteA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume.nombreMatchs").value(1));
    }

    @Test
    void siteAdminCannotReadPlayersFromMatchOnAnotherSite() throws Exception {
        mockMvc.perform(get("/match/{id}/joueurs", matchB.getId())
                        .header("Authorization", bearer(adminSiteA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acces refuse"));
    }

    private String bearer(Membre membre) {
        return "Bearer " + jwtService.generateToken(membre);
    }

    private Site site(String name) {
        return Site.builder()
                .name(name)
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .build();
    }

    private Terrain terrain(String name, Site site) {
        return Terrain.builder().nom(name).site(site).build();
    }

    private Match match(Terrain terrain, Membre organisateur, int daysFromNow) {
        return Match.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .dateHeureDebut(LocalDateTime.now().plusDays(daysFromNow))
                .estPublic(true)
                .statut(StatutMatch.PLANIFIE)
                .build();
    }

    private Reservation reservation(Match match, Membre membre) {
        return Reservation.builder()
                .match(match)
                .membre(membre)
                .dateReservation(LocalDateTime.now())
                .statut(StatutReservation.CONFIRMEE)
                .build();
    }

    private MembreGlobal global(String matricule, Role role) {
        MembreGlobal membre = new MembreGlobal();
        fillMember(membre, matricule, role);
        return membre;
    }

    private MembreSite siteMember(String matricule, Role role, Site site) {
        MembreSite membre = new MembreSite();
        fillMember(membre, matricule, role);
        membre.setSite(site);
        return membre;
    }

    private void fillMember(Membre membre, String matricule, Role role) {
        membre.setMatricule(matricule);
        membre.setNom("Nom");
        membre.setPrenom("Prenom");
        membre.setEmail(matricule + "@example.test");
        membre.setRole(role);
    }
}
