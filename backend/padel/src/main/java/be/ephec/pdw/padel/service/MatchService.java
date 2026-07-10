package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.constants.BusinessConstants;
import be.ephec.pdw.padel.exception.BusinessRuleException;
import be.ephec.pdw.padel.dto.JoueurDTO;
import be.ephec.pdw.padel.dto.MatchReponseDTO;
import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {
    private static final int NOMBRE_JOUEURS_REQUIS = BusinessConstants.MAX_PLAYERS_PER_MATCH;

    private final MembreRepository membreRepository;
    private final TerrainRepository terrainRepository;
    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final TerrainService terrainService;



    public Match creerMatch(String matricule, Long idTerrain, LocalDateTime dateHeure, boolean estPublic) {

        Membre membre = membreRepository.findById(matricule)
                .orElseThrow(() -> new BusinessRuleException("Membre n'existe pas"));

        // pénalité et dette
        if (membre.aUnePenaliteActive()) {
            throw new BusinessRuleException("Le membre a une pénalité");
        }
        if (membre.getSoldeDu() > 0) {
            throw new BusinessRuleException("Le membre a un solde du de " + membre.getSoldeDu() + "€");
        }

        Terrain terrain = terrainRepository.findById(idTerrain)
                .orElseThrow(() -> new BusinessRuleException("Terrain introuvable"));

        if (matchRepository.existsByTerrainAndDateHeureDebut(terrain, dateHeure)) {
            throw new BusinessRuleException("Terrain déjà réservé à cette heure");
        }

        terrainService.verifierJourFermeture(terrain.getSite(), dateHeure);
        validerDroitReservation(membre, terrain, dateHeure);

        Match match = Match.builder()
                .organisateur(membre)
                .terrain(terrain)
                .dateHeureDebut(dateHeure)
                .statut(StatutMatch.PLANIFIE)
                .estPublic(estPublic)
                .build();

        matchRepository.save(match);

        Reservation reservation = Reservation.builder()
                .match(match)
                .membre(membre)
                .dateReservation(LocalDateTime.now())
                .statut(StatutReservation.CONFIRMEE)
                .build();

        reservationRepository.save(reservation);
        log.info("Match créé par {} sur terrain {} à {}", matricule, idTerrain, dateHeure);
        return match;
    }

    @Transactional
    public List<MatchReponseDTO> matchsDisponibles(){

        LocalDateTime maintenant = LocalDateTime.now();
        List<Match> matchs = matchRepository.findAll()
                .stream()
                .filter(m -> m.getDateHeureDebut() != null)
                .filter(m -> m.getDateHeureDebut().isAfter(maintenant))
                .toList();

        log.info("Nombre de matchs trouvés: {}", matchs.size());

        return matchs.stream()
                .filter(this::matchAffichable)
                .filter(m -> m.getStatut() != StatutMatch.ANNULE)
                .filter(m -> estVisibleCommeMatchPublic(m, maintenant))
                .filter(m -> nombreParticipantsVisibles(m) < NOMBRE_JOUEURS_REQUIS)
                .map(m -> toMatchReponseDTO(m, estVisibleCommeMatchPublic(m, maintenant)))
                .toList();
    }

    private boolean matchAffichable(Match match) {
        return match.getTerrain() != null
                && match.getTerrain().getSite() != null
                && match.getOrganisateur() != null;
    }

    private boolean estVisibleCommeMatchPublic(Match match, LocalDateTime maintenant) {
        if (match.isEstPublic()) {
            return true;
        }

        LocalDateTime veille = match.getDateHeureDebut().minusDays(1);
        return !maintenant.isBefore(veille)
                && nombreReservationsPayees(match) < NOMBRE_JOUEURS_REQUIS;
    }

    private MatchReponseDTO toMatchReponseDTO(Match match, boolean estPublic) {
        return new MatchReponseDTO(
                match.getId(),
                match.getDateHeureDebut(),
                nombreParticipantsVisibles(match),
                match.getTerrain().getNom(),
                match.getTerrain().getSite().getId(),
                match.getTerrain().getSite().getName(),
                match.getOrganisateur().getMatricule(),
                estPublic,
                match.getStatut()
        );
    }

    public List<JoueurDTO> joueursInscritMatch(Long Matchid){
        Match match = matchRepository.findById(Matchid)
                .orElseThrow(() -> new BusinessRuleException("Match introuvable"));

        return match.getReservations()
                .stream()
                .map(Reservation::getMembre)
                .map(membre -> JoueurDTO.builder()
                        .matricule(membre.getMatricule())
                        .nom(membre.getNom())
                        .prenom(membre.getPrenom())
                        .build())
                .toList();
    }


    /**
     * methode générale
     * met a jour l'état d'un match normalement
     * @param match le match à mettre à jour
     */
    @Transactional
    public void mettreAJourEtatMatch(Match match){
        mettreAJourEtatMatch(match, null);
    }

    /**
     * cas spécial
     * met à jour l'état d'un match en permettant d'ignorer temporairement une réservation pendant le traitement d'un paiement
     * @param match le match à mettre à jour
     * @param reservationEnCoursPaiementId id de la réservation actuellement en cours de paiement (null si aucun cas particulier)
     */
    @Transactional
    public void mettreAJourEtatMatch(Match match, Long reservationEnCoursPaiementId){
        long nbJoueursPayes = reservationRepository.countByMatchAndEstPayeeTrue(match);
        long nbJoueursNonPayes = reservationRepository.countByMatchAndEstPayeeFalse(match);
        LocalDateTime veille = match.getDateHeureDebut().minusDays(1);
        LocalDateTime maintenant = LocalDateTime.now();


         //verification la veille
        if (!maintenant.isBefore(veille)) {
            //Supprime les réservations non payées et recalcul du nb joueurs payés
            if (nbJoueursNonPayes > 0) {
                supprimerReservationsNonPayees(match, reservationEnCoursPaiementId);
                nbJoueursPayes = reservationRepository.countByMatchAndEstPayeeTrue(match);
            }

            //si était complet avant verification redevient planifié
            if (nbJoueursPayes < NOMBRE_JOUEURS_REQUIS && match.getStatut() == StatutMatch.COMPLET) {
                match.setStatut(StatutMatch.PLANIFIE);
                matchRepository.save(match);
            }

            //devient public si privé et incomplet
            if(!match.isEstPublic() && nbJoueursPayes < NOMBRE_JOUEURS_REQUIS){
                match.setEstPublic(true);
                match.setStatut(StatutMatch.PLANIFIE);
                matchRepository.save(match);
            }
        }

        //vérification début du match
        if (!maintenant.isBefore(match.getDateHeureDebut())) {
            //suppresion final des non payés
            supprimerReservationsNonPayees(match, reservationEnCoursPaiementId);
            nbJoueursPayes = reservationRepository.countByMatchAndEstPayeeTrue(match);

            //pénalité organisateur et ajout du solde du
            if (nbJoueursPayes < NOMBRE_JOUEURS_REQUIS){
                Membre organisateur = match.getOrganisateur();
                organisateur.setPenaliteActive(true);
                organisateur.setFinPenalite(maintenant.plusDays(BusinessConstants.PENALTY_DURATION_DAYS));

                double total = BusinessConstants.MATCH_TOTAL_PRICE;
                double dejaPaye = nbJoueursPayes * BusinessConstants.MATCH_PRICE_PER_PLAYER;
                double reste = total - dejaPaye;
                organisateur.setSoldeDu(reste);
                membreRepository.save(organisateur);
            }
        }
    }

    private void supprimerReservationsNonPayees(Match match, Long reservationEnCoursPaiementId) {
        if (reservationEnCoursPaiementId == null) {
            reservationRepository.deleteByMatchAndEstPayeeFalse(match);
            return;
        }

        reservationRepository.deleteByMatchAndEstPayeeFalseAndIdNot(match, reservationEnCoursPaiementId);
    }

    public void validerDroitReservation(Membre membre, Terrain terrain, LocalDateTime dateMatch) {
        validerCategorieMembre(membre);
        validerSiteReservation(membre, terrain);
        validerFenetreReservation(membre, dateMatch);
    }

    private void validerCategorieMembre(Membre membre) {
        String matricule = membre.getMatricule();

        if (membre instanceof MembreGlobal && matricule != null && matricule.startsWith("G")) {
            return;
        }

        if (membre instanceof MembreSite && matricule != null && matricule.startsWith("S")) {
            return;
        }

        if (membre instanceof MembreLibre && matricule != null && matricule.startsWith("L")) {
            return;
        }

        throw new BusinessRuleException("Le matricule ne correspond pas a la categorie du membre");
    }

    private void validerSiteReservation(Membre membre, Terrain terrain) {
        if (!(membre instanceof MembreSite membreSite)) {
            return;
        }

        Site siteMembre = membreSite.getSite();
        Site siteTerrain = terrain.getSite();

        if (siteMembre == null || siteTerrain == null || !memeSite(siteMembre, siteTerrain)) {
            throw new BusinessRuleException("Un membre du site ne peut reserver que sur son site");
        }
    }

    private boolean memeSite(Site siteMembre, Site siteTerrain) {
        if (siteMembre.getId() != null && siteTerrain.getId() != null) {
            return siteMembre.getId().equals(siteTerrain.getId());
        }

        return siteMembre == siteTerrain;
    }

    public int nombreParticipantsVisibles(Match match) {
        long nbParticipants = match.isEstPublic()
                ? nombrePlacesOccupeesMatchPublic(match)
                : reservationsDuMatch(match).size();

        return (int) nbParticipants;
    }

    private long nombrePlacesOccupeesMatchPublic(Match match) {
        long nbPlacesOccupees = nombreReservationsPayees(match);

        boolean organisateurNonPaye = reservationsDuMatch(match)
                .stream()
                .anyMatch(r -> !r.isEstPayee()
                        && r.getMembre() != null
                        && r.getMembre().getMatricule().equals(match.getOrganisateur().getMatricule()));

        if (organisateurNonPaye) {
            nbPlacesOccupees++;
        }

        return nbPlacesOccupees;
    }

    private long nombreReservationsPayees(Match match) {
        return reservationsDuMatch(match)
                .stream()
                .filter(Reservation::isEstPayee)
                .count();
    }

    private List<Reservation> reservationsDuMatch(Match match) {
        return match.getReservations() == null ? List.of() : match.getReservations();
    }

    private void validerFenetreReservation(Membre membre, LocalDateTime dateMatch) {
        long joursDelai = membre.getDelaiReservations();
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime dateMax = maintenant.plusDays(joursDelai);

        if (dateMatch.isBefore(maintenant)) {
            throw new BusinessRuleException("Impossible de reserver un match dans le passé");
        }

        if (dateMatch.isAfter(dateMax)) {
            throw new BusinessRuleException("Ce membre ne peut pas reserver plus de " + joursDelai + " jours a l'avance");
        }
    }

}
