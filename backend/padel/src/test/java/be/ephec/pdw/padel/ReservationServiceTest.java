package be.ephec.pdw.padel;

import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.Reservation;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private Match match;

    @BeforeEach
    void setup() {
        organisateur = new MembreGlobal();
        organisateur.setMatricule("G0002");

        match = new Match();
        match.setId(10L);
        match.setOrganisateur(organisateur);
        match.setEstPublic(true);
        match.setDateHeureDebut(LocalDateTime.now().plusHours(10));
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
}
