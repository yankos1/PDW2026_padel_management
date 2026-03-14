package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.dto.JoueurDTO;
import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MembreRepository membreRepository;
    private final TerrainRepository terrainRepository;
    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final JourFermetureRepository jourFermetureRepository;


    public Match creerMatch(String matricule, Long idTerrain, LocalDateTime dateHeure, boolean estPublic) {

        Membre membre = membreRepository.findById(matricule).orElseThrow(() -> new RuntimeException("Membre n'existe pas"));

        if (membre.aUnePenaliteActive()) {
            throw new RuntimeException("Le membre a une pénalité");
        }

        Terrain terrain = terrainRepository.findById(idTerrain).orElseThrow(() -> new RuntimeException("Terrain introuvable"));

        if (matchRepository.existsByTerrainAndDateHeureDebut(terrain, dateHeure)) {
            throw new RuntimeException("Terrain déjà réservé à cette heure");
        }

        verifierJourFermeture(terrain.getSite(), dateHeure);
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
        return match;

    }

    public List<Match> matchsDisponibles(){

        List<Match> matchs = matchRepository.findByStatutAndEstPublic(StatutMatch.PLANIFIE, true);

        matchs.forEach(this::mettreAJourEtatMatch);

        return matchs.stream()
                .filter(m -> m.getReservations().size() < 4)
                .toList();
    }

    public List<JoueurDTO> joueursInscritMatch(Long Matchid){
        Match match = matchRepository.findById(Matchid)
                .orElseThrow(() -> new RuntimeException("Match introuvable"));

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

    public void mettreAJourEtatMatch(Match match){
        long nbJoeursInscrits = match.getReservations().size();
        LocalDateTime veille = match.getDateHeureDebut().minusDays(1);

        if (LocalDateTime.now().isAfter(veille)) {

            // si - de 4 joueurs et privé
            if(!match.isEstPublic() && nbJoeursInscrits < 4){
                match.setEstPublic(true);
            }

            // enlever les joeurs non payé
            match.getReservations().removeIf(reservation -> !reservation.isEstPayee());

            nbJoeursInscrits = match.getReservations().size();

            // pénalité organisateur pour match non complet
            if (nbJoeursInscrits > 4){
                Membre organisateur = match.getOrganisateur();
                organisateur.setPenaliteActive(true);
                organisateur.setFinPenalite(LocalDateTime.now().plusDays(1));
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
            throw new RuntimeException("Ce membre doit réserver au moins " + joursDelai + " jours à l'avance");
        }
    }

    private void verifierJourFermeture(Site site, LocalDateTime dateMatch){
        LocalDate date = dateMatch.toLocalDate();

        if(jourFermetureRepository.existsByDate(date)){
            throw new RuntimeException("Les sites sont fermé ce jour");
        }

        if (jourFermetureRepository.existsBySiteAndDate(site, date)){
            throw new RuntimeException("Ce site est fermé ce jour");

        }
    }

}
