package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
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
            throw new BusinessRuleException("Match est privé");

        if (match.getStatut() == StatutMatch.ANNULE)
            throw new BusinessRuleException("Match annulé");

        long nbJoueursInscrits = match.getReservations().size();
        if (nbJoueursInscrits >= 4)
            throw new BusinessRuleException("Match complet");

        if (reservationRepository.existsByMatchAndMembre(match, membre))
            throw new BusinessRuleException("membre déja inscrit a ce math");

        if (membre.aUnePenaliteActive())
            throw new BusinessRuleException("Le membre a une pénalité");

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

        log.info("Le membre {} rejoint le match {}", matricule, matchId);

        return reservationRepository.save(reservation);
    }

    public Reservation payerReservation(Long reservationId){
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("reservation introuvable"));

        if (reservation.isEstPayee())
            throw new BusinessRuleException("reservation déja payée");

        Match match = reservation.getMatch();
        matchService.mettreAJourEtatMatch(match);

        if(match.getStatut() == StatutMatch.ANNULE){
            throw new BusinessRuleException("Impossible de payer un match annulé");
        }

        reservation.setEstPayee(true);
        reservation.setDatePaiement(LocalDateTime.now());
        reservation.setMontant(15);// prix du match par personne si 4
        reservation.setStatut(StatutReservation.CONFIRMEE);

        log.info("Paiement effectué pour la réservation {}", reservationId);
        return  reservationRepository.save(reservation);
    }
}
