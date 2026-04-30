package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
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
        validerDelaiReservation(membre, dateHeure);

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

    public List<MatchReponseDTO> matchsDisponibles(){

        List<Match> matchs = matchRepository.findByStatutAndEstPublic(StatutMatch.PLANIFIE, true);

        log.info("Nombre de matchs trouvés: {}", matchs.size());

        return matchs.stream()
                .filter(m ->  m.getReservations()
                        .stream()
                        .filter(Reservation::isEstPayee)
                        .count() < 4)
                .map(m -> new MatchReponseDTO(
                        m.getId(),
                        m.getDateHeureDebut(),
                        (int) m.getReservations()
                                .stream()
                                .filter(Reservation::isEstPayee)
                                .count(),
                        m.getTerrain().getNom(),
                        m.isEstPublic()
                ))
                .toList();
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

    @Transactional
    public void mettreAJourEtatMatch(Match match){
        long nbJoeursInscrits = reservationRepository.countByMatch(match);
        LocalDateTime veille = match.getDateHeureDebut().minusDays(1);

        if (LocalDateTime.now().isAfter(veille)) {

            // si - de 4 joueurs et privé
            if(!match.isEstPublic() && nbJoeursInscrits < 4){
                match.setEstPublic(true);
            }

            // enlever les joeurs non payé
            reservationRepository.deleteByMatchAndEstPayeeFalse(match);

            nbJoeursInscrits = reservationRepository.countByMatch(match); //recalcul du nb joeurs

            // pénalité organisateur pour match non complet
            if (nbJoeursInscrits < 4){
                Membre organisateur = match.getOrganisateur();
                organisateur.setPenaliteActive(true);
                organisateur.setFinPenalite(LocalDateTime.now().plusDays(7));
                membreRepository.save(organisateur);
            }

            // gestion du soldeDu
            double total = 60;
            double dejaPaye = nbJoeursInscrits * 15;
            double reste = total-dejaPaye;

            if (reste>0){
                Membre organisateur = match.getOrganisateur();
                organisateur.setSoldeDu(reste);
            }
        }
    }

    private void validerDelaiReservation(Membre membre, LocalDateTime dateMatch){

        long joursDelai = membre.getDelaiReservations();
        LocalDateTime dateMin = LocalDateTime.now().plusDays(joursDelai);

        if(dateMatch.isBefore(dateMin)){
            throw new BusinessRuleException("Ce membre doit réserver au moins " + joursDelai + " jours à l'avance");
        }
    }

}
