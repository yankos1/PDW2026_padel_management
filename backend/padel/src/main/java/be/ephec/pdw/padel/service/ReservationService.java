package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final MatchRepository matchRepository;
    private final MembreRepository membreRepository;
    private final ReservationRepository reservationRepository;
    private final MatchService matchService;

    public Reservation rejoindreMatch (String matricule, Long matchId){
        Match match = matchRepository.findById(matchId).orElseThrow();
        Membre membre = membreRepository.findById(matricule).orElseThrow();

        matchService.mettreAJourEtatMatch(match);

        if (!match.isEstPublic())
            throw new RuntimeException("Match est privé");

        if (match.getStatut() == StatutMatch.ANNULE)
            throw new RuntimeException("Match annulé");

        long nbJoueursInscrits = match.getReservations().size();
        if (nbJoueursInscrits >= 4)
            throw new RuntimeException("Match complet");

        if (reservationRepository.existsByMatchAndMembre(match, membre))
            throw new RuntimeException("membre déja inscrit a ce math");

        Reservation reservation = Reservation.builder()
                .match(match)
                .membre(membre)
                .dateReservation(LocalDateTime.now())
                .statut(StatutReservation.EN_ATTENTE)
                .build();

        if (nbJoueursInscrits == 3){
            match.setStatut(StatutMatch.COMPLET);
            matchRepository.save(match);
        }

        return reservationRepository.save(reservation);
    }

    public Reservation payerReservation(Long reservationId){
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("reservation introuvable"));

        if (reservation.isEstPayee())
            throw new RuntimeException("reservation déja payée");

        Match match = reservation.getMatch();
        matchService.mettreAJourEtatMatch(match);

        if(match.getStatut() == StatutMatch.ANNULE){
            throw new RuntimeException("Impossible de payer un match annulé");
        }

        reservation.setEstPayee(true);
        reservation.setDatePaiement(LocalDateTime.now());
        reservation.setMontant(15);// prix du match par personne si 4
        reservation.setStatut(StatutReservation.CONFIRMEE);

        return  reservationRepository.save(reservation);
    }

}
