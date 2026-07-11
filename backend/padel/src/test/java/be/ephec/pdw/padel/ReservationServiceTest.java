package be.ephec.pdw.padel;

import be.ephec.pdw.padel.exception.BusinessRuleException;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.model.StatutMatch;
import be.ephec.pdw.padel.model.StatutReservation;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.service.MatchService;
import be.ephec.pdw.padel.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService")
class ReservationServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MembreRepository membreRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private MatchService matchService;

    @InjectMocks
    private ReservationService reservationService;

    private Membre organisateur;
    private Membre joueur;
    private Match match;

    @BeforeEach
    void setup() {
        organisateur = new MembreGlobal();
        organisateur.setMatricule("G0002");

        joueur = new MembreGlobal();
        joueur.setMatricule("G0003");

        match = new Match();
        match.setId(10L);
        match.setOrganisateur(organisateur);
        match.setEstPublic(true);
        match.setStatut(StatutMatch.PLANIFIE);
        match.setDateHeureDebut(LocalDateTime.now().plusHours(10));
    }

    @Test
    void shouldRejectJoiningFullMatch() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(membreRepository.findById("G0003")).thenReturn(Optional.of(joueur));
        when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(4L);

        assertThrows(BusinessRuleException.class, () ->
                reservationService.rejoindreMatch("G0003", 10L)
        );

        verify(matchService).mettreAJourEtatMatch(match);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldRejectJoiningMatchWhenMemberAlreadyRegistered() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(membreRepository.findById("G0003")).thenReturn(Optional.of(joueur));
        when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(1L);
        when(reservationRepository.existsByMatchAndMembre(match, joueur)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () ->
                reservationService.rejoindreMatch("G0003", 10L)
        );

        verify(matchService).mettreAJourEtatMatch(match);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldRejectJoiningPrivateMatchDirectly() {
        match.setEstPublic(false);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(membreRepository.findById("G0003")).thenReturn(Optional.of(joueur));

        assertThrows(BusinessRuleException.class, () ->
                reservationService.rejoindreMatch("G0003", 10L)
        );

        verify(matchService).mettreAJourEtatMatch(match);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldRejectJoiningCancelledMatch() {
        match.setStatut(StatutMatch.ANNULE);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(membreRepository.findById("G0003")).thenReturn(Optional.of(joueur));

        assertThrows(BusinessRuleException.class, () ->
                reservationService.rejoindreMatch("G0003", 10L)
        );

        verify(matchService).mettreAJourEtatMatch(match);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldRejectJoiningMatchWhenMemberHasActivePenalty() {
        joueur.setPenaliteActive(true);
        joueur.setFinPenalite(LocalDateTime.now().plusDays(1));

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(membreRepository.findById("G0003")).thenReturn(Optional.of(joueur));
        when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(1L);
        when(reservationRepository.existsByMatchAndMembre(match, joueur)).thenReturn(false);

        assertThrows(BusinessRuleException.class, () ->
                reservationService.rejoindreMatch("G0003", 10L)
        );

        verify(matchService).mettreAJourEtatMatch(match);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldExcludeReservationBeingPaidWhenUpdatingMatchState() {
        Reservation reservation = Reservation.builder()
                .id(48L)
                .match(match)
                .membre(organisateur)
                .build();

        when(reservationRepository.findById(48L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(0L);
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.payerReservation(48L);

        assertTrue(result.isEstPayee());
        assertEquals(15, result.getMontant());
        verify(matchService).mettreAJourEtatMatch(match, 48L);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void shouldIncludeMemberDebtInPaymentAndClearBalance() {
        joueur.setSoldeDu(30);
        Reservation reservation = Reservation.builder()
                .id(49L)
                .match(match)
                .membre(joueur)
                .build();

        when(reservationRepository.findById(49L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(1L);
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.payerReservation(49L);

        assertTrue(result.isEstPayee());
        assertEquals(45, result.getMontant());
        assertEquals(0, joueur.getSoldeDu());
        verify(membreRepository).save(joueur);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void shouldMarkMatchCompleteWhenFourthReservationIsPaid() {
        Reservation reservation = Reservation.builder()
                .id(50L)
                .match(match)
                .membre(joueur)
                .build();

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.countByMatchAndEstPayeeTrue(match)).thenReturn(3L);
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.payerReservation(50L);

        assertTrue(result.isEstPayee());
        assertEquals(StatutReservation.CONFIRMEE, result.getStatut());
        assertEquals(StatutMatch.COMPLET, match.getStatut());
        verify(matchRepository).save(match);
        verify(reservationRepository).save(reservation);
    }
}
