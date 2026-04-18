package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
import be.ephec.pdw.padel.dto.MatchReponseDTO;
import be.ephec.pdw.padel.dto.ReservationReponseDTO;
import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    private final MatchRepository matchRepository;
    private final MembreRepository membreRepository;
    private final ReservationRepository reservationRepository;
    private final MatchService matchService;

    public Reservation rejoindreMatch(String matricule, Long matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        Membre membre = membreRepository.findById(matricule).orElseThrow();

        matchService.mettreAJourEtatMatch(match);

        if (match.isEstPublic())
            log.info("Match public");
        //throw new BusinessRuleException("Impossible de rejoindre un match public sans payer");

        if (match.getStatut() == StatutMatch.ANNULE)
            throw new BusinessRuleException("Match annulé");

        long nbJoueursInscrits = reservationRepository.countByMatch(match);
        if (nbJoueursInscrits >= 4)
            throw new BusinessRuleException("Match complet");

        if (reservationRepository.existsByMatchAndMembre(match, membre))
            throw new BusinessRuleException("membre déja inscrit a ce math");

        if (membre.aUnePenaliteActive())
            throw new BusinessRuleException("Le membre a une pénalité");

        if (membre.getSoldeDu() > 0) {
            throw new BusinessRuleException("Le membre a un solde du");
        }


        Reservation reservation = Reservation.builder()
                .match(match)
                .membre(membre)
                .dateReservation(LocalDateTime.now())
                .statut(StatutReservation.EN_ATTENTE)
                .build();

        if (nbJoueursInscrits + 1 == 4) {
            match.setStatut(StatutMatch.COMPLET);
            matchRepository.save(match);
        }

        log.info("Le membre {} rejoint le match {}", matricule, matchId);

        return reservationRepository.save(reservation);
    }

    public Reservation payerReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("reservation introuvable"));


        if (reservation.isEstPayee())
            throw new BusinessRuleException("reservation déja payée");

        Match match = reservation.getMatch();
        matchService.mettreAJourEtatMatch(match);

        if (match.getStatut() == StatutMatch.ANNULE) {
            throw new BusinessRuleException("Impossible de payer un match annulé");
        }


        long nbJoueurs;

        if (match.isEstPublic()) {
            nbJoueurs = match.getReservations()
                    .stream()
                    .filter(Reservation::isEstPayee)
                    .count();
        } else {
            nbJoueurs = reservationRepository.countByMatch(match);
        }

        // gestion de dette
        Membre membre = reservation.getMembre();
        double montant = 15; // prix du match par personne si 4

        if (membre.getSoldeDu() > 0) {
            montant += membre.getSoldeDu();
            membre.setSoldeDu(0);
        }

        reservation.setEstPayee(true);
        reservation.setDatePaiement(LocalDateTime.now());
        reservation.setMontant(montant);
        reservation.setStatut(StatutReservation.CONFIRMEE);

        if (nbJoueurs + 1 == 4) {
            match.setStatut(StatutMatch.COMPLET);
            matchRepository.save(match);
        }

        membreRepository.save(membre);
        log.info("Paiement effectué pour la réservation {}", reservationId);
        return reservationRepository.save(reservation);
    }

    public List<ReservationReponseDTO> getReservationsByMembre(String matricule) {
        Membre membre = membreRepository.findById(matricule)
                .orElseThrow(() -> new BusinessRuleException("Membre introuvable"));

        return reservationRepository.findByMembre(membre)
                .stream()
                .map(reservation ->
                        new ReservationReponseDTO(reservation.getId(),
                        new MatchReponseDTO(reservation.getMatch().getId(),
                                reservation.getMatch().getDateHeureDebut(),
                                reservation.getMatch().getReservations().size(),
                                reservation.getMatch().getTerrain().getNom(),
                                reservation.getMatch().isEstPublic()
                        ),
                        reservation.isEstPayee()
                ))
                .toList();
    }
}
