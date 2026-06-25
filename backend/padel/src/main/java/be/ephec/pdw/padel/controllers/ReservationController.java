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
    // TODO [IMPORTANT][SECURITE] Recuperer le membre connecte depuis le JWT, pas depuis le body.
    public Reservation rejoindre(@RequestBody ReservationDTO.PostInput input) {
        return reservationService.rejoindreMatch(input.getMatricule(), input.getMatchId());
    }

    // TODO [IMPORTANT][SECURITE] Verifier cote serveur que l'organisateur provient de l'utilisateur authentifie avant d'ajouter un joueur prive.
    @PostMapping("/match-prive/ajouter-joueur")
    public Reservation ajouterJoueurMatchPrive(@RequestBody ReservationDTO.AjoutJoueurPriveInput input) {
        return reservationService.ajouterJoueurMatchPrive(
                input.getOrganisateurMatricule(),
                input.getJoueurMatricule(),
                input.getMatchId()
        );
    }

    // TODO [IMPORTANT][SECURITE] Verifier que la reservation payee appartient a l'utilisateur connecte ou a un admin.
    @PutMapping("/{id}/payer")
    public Reservation payerReservation(@PathVariable Long id){
        return reservationService.payerReservation(id);
    }

    @GetMapping("/membre/{matricule}")
    // TODO [IMPORTANT][SECURITE] Verifier que le membre demande correspond a l'utilisateur connecte ou a un admin.
    public List<ReservationReponseDTO> getReservationsByMembre(@PathVariable String matricule) {
        return reservationService.getReservationsByMembre(matricule);
    }
}
