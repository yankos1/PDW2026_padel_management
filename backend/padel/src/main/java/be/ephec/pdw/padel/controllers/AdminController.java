package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
import be.ephec.pdw.padel.model.*;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final MembreRepository membreRepository;
    private final TerrainRepository terrainRepository;

    @GetMapping("/matchs")
    public long nombreMatchs(@RequestParam String matricule){

        Membre membre = membreRepository.findById(matricule)
                .orElseThrow();

        CheckAdmin(membre);

        return matchRepository.count();
    }

    private void CheckAdmin(Membre membre) {
        if(membre.getRole() == Role.USER){
            throw new BusinessRuleException("Accès refusé");
        }
    }

    @GetMapping("/ca")
    public double chiffreAffaires(){

        long reservationsPayees = reservationRepository.countByEstPayeeTrue();

        return reservationsPayees * 15;
    }

    @GetMapping("/membres")
    public long nombreMembres(){
        return membreRepository.count();
    }

    @GetMapping("/terrains")
    public long nombreTerrains(){
        return terrainRepository.count();
    }

    @GetMapping("/matchs-complets")
    public long matchsComplets(){
        return matchRepository.countByStatut((StatutMatch.COMPLET));
    }

    @GetMapping("/taux-remplissage")
    public double tauxRemplissage(){

        long totalPlaces = matchRepository.count() * 4;

        long reservations = reservationRepository.count();

        if(totalPlaces == 0) return 0;

        return (double) reservations / totalPlaces * 100;
    }

    @GetMapping("/revenus-par-site")
    public Map<String, Double> revenusParSite(){

        Map<String, Double> revenus = new HashMap<>();
        List<Reservation> reservations = reservationRepository.findAll();

        for(Reservation reservation : reservations){
            if(reservation.isEstPayee()){
                Site site = reservation.getMatch()
                        .getTerrain()
                        .getSite();

                String name = site.getName();

                revenus.put(name,revenus.getOrDefault(name, 0.0)+ reservation.getMontant());
            }
        }
        return revenus;
    }
}
