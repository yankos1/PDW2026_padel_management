package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.ReservationDTO;
import be.ephec.pdw.padel.dto.ReservationReponseDTO;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;


    @PostMapping("/rejoindre")
    public Reservation rejoindre(@RequestBody ReservationDTO.PostInput input) {
        return reservationService.rejoindreMatch(input.getMatricule(), input.getMatchId());
    }

    @PostMapping("/match-prive/ajouter-joueur")
    public Reservation ajouterJoueurMatchPrive(@RequestBody ReservationDTO.AjoutJoueurPriveInput input) {
        return reservationService.ajouterJoueurMatchPrive(
                input.getOrganisateurMatricule(),
                input.getJoueurMatricule(),
                input.getMatchId()
        );
    }

    @PutMapping("/{id}/payer")
    public Reservation payerReservation(@PathVariable Long id){
        return reservationService.payerReservation(id);
    }

    @GetMapping("/membre/{matricule}")
    public List<ReservationReponseDTO> getReservationsByMembre(@PathVariable String matricule) {
        return reservationService.getReservationsByMembre(matricule);
    }
}
