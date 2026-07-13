package be.ephec.pdw.padel;

import be.ephec.pdw.padel.exception.BusinessRuleException;
import be.ephec.pdw.padel.dto.MatchReponseDTO;
import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.service.MatchService;
import be.ephec.pdw.padel.service.CurrentUserService;
import be.ephec.pdw.padel.service.TerrainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService")
// TODO [FACULTATIF][TEST INTEGRATION] Tester la double réservation concurrente
class MatchServiceTest {

    @Mock private MembreRepository membreRepository;
    @Mock private TerrainRepository terrainRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private ReservationRepository reservationRepository;
    @SuppressWarnings("unused")
    @Mock private TerrainService terrainService;
    @Mock private CurrentUserService currentUserService;

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
            ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(reservationCaptor.capture());

            Reservation reservation = reservationCaptor.getValue();
            assertEquals(result, reservation.getMatch());
            assertEquals(membre, reservation.getMembre());
            assertEquals(StatutReservation.CONFIRMEE, reservation.getStatut());
            assertFalse(reservation.isEstPayee());
            assertEquals(0, reservation.getMontant());
            assertEquals(StatutMatch.PLANIFIE, result.getStatut());
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

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(2L);

            matchService.mettreAJourEtatMatch(match);

            assertTrue(match.isEstPublic());
        }

        @Test
        void shouldKeepFutureIncompleteMatchPlanned() {
            Match match = new Match();
            match.setStatut(StatutMatch.PLANIFIE);
            match.setDateHeureDebut(LocalDateTime.now().plusHours(2));

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(2L);

            matchService.synchroniserStatut(match);

            assertEquals(StatutMatch.PLANIFIE, match.getStatut());
            verify(matchRepository, never()).save(match);
        }

        @Test
        void shouldMarkFutureMatchCompleteWithFourPaidReservations() {
            Match match = new Match();
            match.setStatut(StatutMatch.PLANIFIE);
            match.setDateHeureDebut(LocalDateTime.now().plusHours(2));

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(4L);

            matchService.synchroniserStatut(match);

            assertEquals(StatutMatch.COMPLET, match.getStatut());
            verify(matchRepository).save(match);
        }

        @Test
        void shouldNotCompletePublicMatchWithUnpaidReservations() {
            Match match = new Match();
            match.setEstPublic(true);
            match.setStatut(StatutMatch.PLANIFIE);
            match.setDateHeureDebut(LocalDateTime.now().plusHours(2));

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(3L);

            matchService.synchroniserStatut(match);

            assertEquals(StatutMatch.PLANIFIE, match.getStatut());
        }

        @Test
        void shouldNotDoubleCountPaidConfirmedReservationForCompletion() {
            Match match = new Match();
            match.setStatut(StatutMatch.PLANIFIE);
            match.setDateHeureDebut(LocalDateTime.now().plusHours(2));

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(1L);

            matchService.synchroniserStatut(match);

            assertEquals(StatutMatch.PLANIFIE, match.getStatut());
        }

        @Test
        void shouldKeepPrivateUnpaidPlacesBeforeDayBeforeMatch() {
            Match match = new Match();
            match.setEstPublic(false);
            match.setStatut(StatutMatch.PLANIFIE);
            match.setDateHeureDebut(LocalDateTime.now().plusDays(2));
            match.setOrganisateur(membre);

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(1L);
            when(reservationRepository.countByMatchAndEstPayeeFalse(match)).thenReturn(3L);

            matchService.mettreAJourEtatMatch(match);

            assertFalse(match.isEstPublic());
            verify(reservationRepository, never()).deleteByMatchAndEstPayeeFalse(match);
        }

        @Test
        void shouldReleasePrivateUnpaidPlacesWhenMatchBecomesPublicDayBefore() {
            Match match = new Match();
            match.setEstPublic(false);
            match.setStatut(StatutMatch.PLANIFIE);
            match.setDateHeureDebut(LocalDateTime.now().plusHours(10));
            match.setOrganisateur(membre);

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(1L);
            when(reservationRepository.countByMatchAndEstPayeeFalse(match)).thenReturn(3L);

            matchService.mettreAJourEtatMatch(match);

            assertTrue(match.isEstPublic());
            verify(reservationRepository).deleteByMatchAndEstPayeeFalse(match);
        }

        @Test
        void shouldMarkMatchEndedAfterNinetyMinutesAsFinished() {
            Match match = new Match();
            match.setStatut(StatutMatch.PLANIFIE);
            match.setDateHeureDebut(LocalDateTime.now().minusMinutes(91));

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(2L);

            matchService.synchroniserStatut(match);

            assertEquals(StatutMatch.TERMINE, match.getStatut());
            verify(matchRepository).save(match);
        }

        @Test
        void shouldMarkPastCompleteMatchAsFinished() {
            Match match = new Match();
            match.setStatut(StatutMatch.COMPLET);
            match.setDateHeureDebut(LocalDateTime.now().minusMinutes(91));

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(4L);

            matchService.synchroniserStatut(match);

            assertEquals(StatutMatch.TERMINE, match.getStatut());
            verify(matchRepository).save(match);
        }

        @Test
        void shouldKeepCancelledMatchCancelledAfterEnd() {
            Match match = new Match();
            match.setStatut(StatutMatch.ANNULE);
            match.setDateHeureDebut(LocalDateTime.now().minusMinutes(91));

            matchService.synchroniserStatut(match);

            assertEquals(StatutMatch.ANNULE, match.getStatut());
            verify(reservationRepository, never()).countByMatchAndEstPayeeTrue(match);
            verify(matchRepository, never()).save(match);
        }

        @Test
        void shouldNeverUpdateCancelledMatchInBusinessUpdate() {
            Match match = new Match();
            match.setStatut(StatutMatch.ANNULE);
            match.setEstPublic(false);
            match.setDateHeureDebut(LocalDateTime.now().minusMinutes(91));
            match.setOrganisateur(membre);

            matchService.mettreAJourEtatMatch(match);

            assertEquals(StatutMatch.ANNULE, match.getStatut());
            assertFalse(match.isEstPublic());
            verify(reservationRepository, never()).deleteByMatchAndEstPayeeFalse(match);
            verify(matchRepository, never()).save(match);
        }

        @Test
        void shouldReturnOnlyFuturePlannedAvailablePublicMatches() {
            Match available = publicMatch(1L, StatutMatch.PLANIFIE, LocalDateTime.now().plusHours(2), List.of());
            Match complete = publicMatch(2L, StatutMatch.PLANIFIE, LocalDateTime.now().plusHours(2), List.of());
            Match cancelled = publicMatch(3L, StatutMatch.ANNULE, LocalDateTime.now().plusHours(2), List.of());

            when(matchRepository.findDisponiblesCandidates(any())).thenReturn(List.of(available, complete, cancelled));
            when(reservationRepository.countByMatchAndEstPayeeTrue(available)).thenReturn(1L);
            when(reservationRepository.countByMatchAndEstPayeeTrue(complete)).thenReturn(4L);

            List<MatchReponseDTO> result = matchService.matchsDisponibles();

            assertEquals(1, result.size());
            assertEquals(1L, result.getFirst().id());
            assertEquals(StatutMatch.PLANIFIE, result.getFirst().statut());
        }

        @Test
        void shouldNotApplyPenaltyBeforeMatchStarts() {
            Match match = new Match();
            match.setEstPublic(false);
            match.setDateHeureDebut(LocalDateTime.now().plusHours(10));
            match.setOrganisateur(membre);

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(2L);

            matchService.mettreAJourEtatMatch(match);

            assertFalse(membre.isPenaliteActive());
            verify(membreRepository, never()).save(membre);
        }

        @Test
        void shouldApplyPenaltyIfMatchStartedWithoutEnoughPlayers() {
            Match match = new Match();
            match.setEstPublic(true);
            match.setDateHeureDebut(LocalDateTime.now().minusMinutes(1));
            match.setOrganisateur(membre);

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(2L);

            matchService.mettreAJourEtatMatch(match);

            assertTrue(membre.isPenaliteActive());
            assertNotNull(membre.getFinPenalite());
            verify(membreRepository).save(membre);
        }

        @Test
        void shouldSetOrganizerDebtForUnpaidPlacesWhenMatchStartsIncomplete() {
            Match match = new Match();
            match.setEstPublic(true);
            match.setDateHeureDebut(LocalDateTime.now().minusMinutes(1));
            match.setOrganisateur(membre);

            when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(2L);

            matchService.mettreAJourEtatMatch(match);

            assertTrue(membre.isPenaliteActive());
            assertEquals(30, membre.getSoldeDu());
            verify(membreRepository).save(membre);
        }
    }

    private Match publicMatch(Long id, StatutMatch statut, LocalDateTime dateHeureDebut, List<Reservation> reservations) {
        Site site = new Site();
        site.setId(1L);
        site.setName("Bruxelles");

        Terrain terrain = new Terrain();
        terrain.setId(1L);
        terrain.setNom("Central");
        terrain.setSite(site);

        Match match = new Match();
        match.setId(id);
        match.setEstPublic(true);
        match.setStatut(statut);
        match.setDateHeureDebut(dateHeureDebut);
        match.setOrganisateur(membre);
        match.setTerrain(terrain);
        match.setReservations(reservations);
        return match;
    }
}

