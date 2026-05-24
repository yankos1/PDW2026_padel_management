package be.ephec.pdw.padel;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.*;
import be.ephec.pdw.padel.service.MatchService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService")
class MatchServiceTest {

    @Mock private MembreRepository membreRepository;
    @Mock private TerrainRepository terrainRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks
    private MatchService matchService;

    private Membre membre;
    private Terrain terrain;

    @BeforeEach
    void setup() {
        membre = new MembreGlobal();
        membre.setMatricule("G0002");

        Site site = new Site();
        site.setId(1L);

        terrain = new Terrain();
        terrain.setId(1L);
        terrain.setSite(site);
    }

    // ═══════════════════════════════════════
    // 1. CREATION MATCH
    // ═══════════════════════════════════════

    @Nested
    @DisplayName("Création de match")
    class CreationMatch {

        @Test
        void shouldCreateMatchSuccessfully() {
            LocalDateTime date = LocalDateTime.now().plusDays(20);

            when(membreRepository.findById("G0002")).thenReturn(Optional.of(membre));
            when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
            when(matchRepository.existsByTerrainAndDateHeureDebut(any(), any())).thenReturn(false);

            Match result = matchService.creerMatch("G0002", 1L, date, false);

            assertNotNull(result);
            verify(matchRepository).save(any());
            verify(reservationRepository).save(any());
        }

        @Test
        void shouldRejectIfTerrainAlreadyReserved() {
            LocalDateTime date = LocalDateTime.now().plusDays(20);

            when(membreRepository.findById("G0002")).thenReturn(Optional.of(membre));
            when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
            when(matchRepository.existsByTerrainAndDateHeureDebut(any(), any())).thenReturn(true);

            assertThrows(BusinessRuleException.class, () ->
                    matchService.creerMatch("G0002", 1L, date, false)
            );
        }
    }

    // ═══════════════════════════════════════
    // 2. REGLES MEMBRE
    // ═══════════════════════════════════════

    @Nested
    @DisplayName("Règles membre")
    class ReglesMembre {

        @Test
        void shouldRejectIfMembreHasPenalty() {
            membre.setPenaliteActive(true);
            membre.setFinPenalite(LocalDateTime.now().plusDays(1));

            when(membreRepository.findById("G0002")).thenReturn(Optional.of(membre));

            assertThrows(BusinessRuleException.class, () ->
                    matchService.creerMatch("G0002", 1L, LocalDateTime.now().plusDays(20), false)
            );
        }

        @Test
        void shouldRejectIfMembreHasDebt() {
            membre.setSoldeDu(20);

            when(membreRepository.findById("G0002")).thenReturn(Optional.of(membre));

            assertThrows(BusinessRuleException.class, () ->
                    matchService.creerMatch("G0002", 1L, LocalDateTime.now().plusDays(20), false)
            );
        }
    }

    // ═══════════════════════════════════════
    // 3. DELAI RESERVATION
    // ═══════════════════════════════════════

    @Nested
    @DisplayName("Délai réservation")
    class DelaiReservation {

        @Test
        void shouldRejectIfReservationTooEarly() {
            LocalDateTime date = LocalDateTime.now().plusDays(22); // trop tôt pour global

            when(membreRepository.findById("G0002")).thenReturn(Optional.of(membre));
            when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
            when(matchRepository.existsByTerrainAndDateHeureDebut(any(), any())).thenReturn(false);

            assertThrows(BusinessRuleException.class, () ->
                    matchService.creerMatch("G0002", 1L, date, false)
            );
        }

        @Test
        void shouldAllowGlobalMemberInside21DaysWindow() {
            LocalDateTime date = LocalDateTime.now().plusDays(10);

            when(membreRepository.findById("G0002")).thenReturn(Optional.of(membre));
            when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
            when(matchRepository.existsByTerrainAndDateHeureDebut(any(), any())).thenReturn(false);

            Match result = matchService.creerMatch("G0002", 1L, date, false);

            assertNotNull(result);
        }

        @Test
        void shouldRejectLibreMemberOutside5DaysWindow() {
            MembreLibre membreLibre = new MembreLibre();
            membreLibre.setMatricule("L00001");

            when(membreRepository.findById("L00001")).thenReturn(Optional.of(membreLibre));
            when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
            when(matchRepository.existsByTerrainAndDateHeureDebut(any(), any())).thenReturn(false);

            assertThrows(BusinessRuleException.class, () ->
                    matchService.creerMatch("L00001", 1L, LocalDateTime.now().plusDays(6), false)
            );
        }

        @Test
        void shouldAllowSiteMemberOnOwnSite() {
            Site site = new Site();
            site.setId(1L);

            MembreSite membreSite = new MembreSite();
            membreSite.setMatricule("S00001");
            membreSite.setSite(site);
            terrain.setSite(site);

            when(membreRepository.findById("S00001")).thenReturn(Optional.of(membreSite));
            when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
            when(matchRepository.existsByTerrainAndDateHeureDebut(any(), any())).thenReturn(false);

            Match result = matchService.creerMatch("S00001", 1L, LocalDateTime.now().plusDays(13), false);

            assertNotNull(result);
            verify(matchRepository).save(any());
        }

        @Test
        void shouldRejectSiteMemberOnAnotherSite() {
            Site memberSite = new Site();
            memberSite.setId(1L);
            Site terrainSite = new Site();
            terrainSite.setId(2L);

            MembreSite membreSite = new MembreSite();
            membreSite.setMatricule("S00001");
            membreSite.setSite(memberSite);
            terrain.setSite(terrainSite);

            when(membreRepository.findById("S00001")).thenReturn(Optional.of(membreSite));
            when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
            when(matchRepository.existsByTerrainAndDateHeureDebut(any(), any())).thenReturn(false);

            assertThrows(BusinessRuleException.class, () ->
                    matchService.creerMatch("S00001", 1L, LocalDateTime.now().plusDays(13), false)
            );
        }

        @Test
        void shouldRejectIfMatriculeDoesNotMatchMemberCategory() {
            membre.setMatricule("S00001");

            when(membreRepository.findById("S00001")).thenReturn(Optional.of(membre));
            when(terrainRepository.findById(1L)).thenReturn(Optional.of(terrain));
            when(matchRepository.existsByTerrainAndDateHeureDebut(any(), any())).thenReturn(false);

            assertThrows(BusinessRuleException.class, () ->
                    matchService.creerMatch("S00001", 1L, LocalDateTime.now().plusDays(20), false)
            );
        }
    }

    // ═══════════════════════════════════════
    // 4. LOGIQUE MATCH
    // ═══════════════════════════════════════

    @Nested
    @DisplayName("Logique match")
    class LogiqueMatch {

        @Test
        void shouldMakeMatchPublicIfNotEnoughPlayers() {
            Match match = new Match();
            match.setEstPublic(false);
            match.setDateHeureDebut(LocalDateTime.now().plusHours(10));
            match.setOrganisateur(membre);

            when(reservationRepository.countByMatch(match)).thenReturn(2L);

            matchService.mettreAJourEtatMatch(match);

            assertTrue(match.isEstPublic());
        }

        @Test
        void shouldApplyPenaltyIfNotEnoughPlayers() {
            Match match = new Match();
            match.setEstPublic(false);
            match.setDateHeureDebut(LocalDateTime.now().plusHours(10));
            match.setOrganisateur(membre);

            when(reservationRepository.countByMatch(match)).thenReturn(2L);

            matchService.mettreAJourEtatMatch(match);

            assertTrue(membre.isPenaliteActive());
            verify(membreRepository).save(membre);
        }
    }
}

