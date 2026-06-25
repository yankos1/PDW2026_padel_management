package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
import be.ephec.pdw.padel.dto.MatchReponseDTO;
import be.ephec.pdw.padel.dto.ReservationReponseDTO;
import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    private static final int NOMBRE_JOUEURS_REQUIS = 4;
    // TODO [IMPORTANT][ARCHITECTURE] Centraliser le prix par joueur et le nombre maximal de joueurs pour eviter les valeurs metier dupliquees.

    private final MatchRepository matchRepository;
    private final MembreRepository membreRepository;
    private final ReservationRepository reservationRepository;
    private final MatchService matchService;

    @Transactional
    public Reservation rejoindreMatch(String matricule, Long matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new BusinessRuleException("Match introuvable"));
        Membre membre = membreRepository.findById(matricule).orElseThrow(() -> new BusinessRuleException("Membre introuvable"));

        matchService.mettreAJourEtatMatch(match);

        if (!match.isEstPublic())
            throw new BusinessRuleException("Seul l'organisateur du match peut ajouter des joueurs a un match prive");

        log.info("Match public");

        if (match.getStatut() == StatutMatch.ANNULE)
            throw new BusinessRuleException("Match annulé");

        long nbPlacesOccupees = nombrePlacesOccupeesMatchPublic(match);
        if (nbPlacesOccupees >= NOMBRE_JOUEURS_REQUIS)
            throw new BusinessRuleException("Match complet");

        if (reservationRepository.existsByMatchAndMembre(match, membre))
            throw new BusinessRuleException("membre déja inscrit a ce math");

        if (membre.aUnePenaliteActive())
            throw new BusinessRuleException("Le membre a une pénalité");

        matchService.validerDroitReservation(membre, match.getTerrain(), match.getDateHeureDebut());

        Reservation reservation = Reservation.builder()
                .match(match)
                .membre(membre)
                .dateReservation(LocalDateTime.now())
                .statut(StatutReservation.EN_ATTENTE)
                .build();

        log.info("Le membre {} rejoint le match {}", matricule, matchId);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation ajouterJoueurMatchPrive(String organisateurMatricule, String joueurMatricule, Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessRuleException("Match introuvable"));
        Membre organisateur = membreRepository.findById(organisateurMatricule)
                .orElseThrow(() -> new BusinessRuleException("Organisateur introuvable"));
        Membre joueur = membreRepository.findById(joueurMatricule)
                .orElseThrow(() -> new BusinessRuleException("Membre introuvable"));

        matchService.mettreAJourEtatMatch(match);

        if (match.isEstPublic()) {
            throw new BusinessRuleException("Le match est public, les membres doivent s'inscrire eux-memes");
        }

        if (!organisateur.getMatricule().equals(match.getOrganisateur().getMatricule())) {
            throw new BusinessRuleException("Seul l'organisateur du match peut ajouter des joueurs");
        }

        if (match.getStatut() == StatutMatch.ANNULE) {
            throw new BusinessRuleException("Match annule");
        }

        long nbJoueursInscrits = reservationRepository.countByMatch(match);
        if (nbJoueursInscrits >= NOMBRE_JOUEURS_REQUIS) {
            throw new BusinessRuleException("Match complet");
        }

        if (reservationRepository.existsByMatchAndMembre(match, joueur)) {
            throw new BusinessRuleException("membre deja inscrit a ce match");
        }

        if (joueur.aUnePenaliteActive()) {
            throw new BusinessRuleException("Le membre a une penalite");
        }

        matchService.validerDroitReservation(joueur, match.getTerrain(), match.getDateHeureDebut());

        Reservation reservation = Reservation.builder()
                .match(match)
                .membre(joueur)
                .dateReservation(LocalDateTime.now())
                .statut(StatutReservation.CONFIRMEE)
                .build();

        log.info("Le membre {} ajoute le membre {} au match prive {}", organisateurMatricule, joueurMatricule, matchId);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation payerReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("reservation introuvable"));


        if (reservation.isEstPayee())
            throw new BusinessRuleException("reservation déja payée");

        Match match = reservation.getMatch();
        matchService.mettreAJourEtatMatch(match, reservation.getId());

        if (match.getStatut() == StatutMatch.ANNULE) {
            throw new BusinessRuleException("Impossible de payer un match annulé");
        }


        long nbJoueurs;

        nbJoueurs = reservationRepository.countByMatchAndEstPayeeTrue(match);

        if (match.isEstPublic() && !estReservationOrganisateur(reservation)
                && nombrePlacesOccupeesMatchPublic(match) >= NOMBRE_JOUEURS_REQUIS) {
            throw new BusinessRuleException("Match complet: les 4 places payees sont deja prises");
        }

        if (match.isEstPublic() && estReservationOrganisateur(reservation)
                && nbJoueurs >= NOMBRE_JOUEURS_REQUIS) {
            throw new BusinessRuleException("Match complet: les 4 places payees sont deja prises");
        }

        if (!match.isEstPublic() && nbJoueurs >= NOMBRE_JOUEURS_REQUIS) {
            throw new BusinessRuleException("Match complet: les 4 places payees sont deja prises");
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

        if (nbJoueurs + 1 == NOMBRE_JOUEURS_REQUIS) {
            match.setStatut(StatutMatch.COMPLET);
            matchRepository.save(match);
        }

        membreRepository.save(membre);
        log.info("Paiement effectué pour la réservation {}", reservationId);
        return reservationRepository.save(reservation);
    }

    private long nombrePlacesOccupeesMatchPublic(Match match) {
        long nbPlacesOccupees = reservationRepository.countByMatchAndEstPayeeTrue(match);

        if (reservationRepository.existsByMatchAndMembreAndEstPayeeFalse(match, match.getOrganisateur())) {
            nbPlacesOccupees++;
        }

        return nbPlacesOccupees;
    }

    private boolean estReservationOrganisateur(Reservation reservation) {
        return reservation.getMembre()
                .getMatricule()
                .equals(reservation.getMatch().getOrganisateur().getMatricule());
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
                                matchService.nombreParticipantsVisibles(reservation.getMatch()),
                                reservation.getMatch().getTerrain().getNom(),
                                reservation.getMatch().getTerrain().getSite().getId(),
                                reservation.getMatch().getTerrain().getSite().getName(),
                                reservation.getMatch().getOrganisateur().getMatricule(),
                                reservation.getMatch().isEstPublic()
                        ),
                        reservation.isEstPayee()
                ))
                .toList();
    }
}
