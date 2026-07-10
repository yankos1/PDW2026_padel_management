package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.exception.ForbiddenException;
import be.ephec.pdw.padel.service.CurrentUserService;
import be.ephec.pdw.padel.dto.ReservationDTO;
import be.ephec.pdw.padel.dto.ReservationReponseDTO;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.repositories.ReservationRepository;
import be.ephec.pdw.padel.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final CurrentUserService currentUserService;


    @PostMapping("/rejoindre")
    public Reservation rejoindre(@Valid @RequestBody ReservationDTO.PostInput input) {
        return reservationService.rejoindreMatch(currentUserService.getCurrentUser().getMatricule(), input.getMatchId());
    }

    @PostMapping("/match-prive/ajouter-joueur")
    public Reservation ajouterJoueurMatchPrive(@Valid @RequestBody ReservationDTO.AjoutJoueurPriveInput input) {
        String organisateurMatricule = currentUserService.getCurrentUser().getMatricule();

        return reservationService.ajouterJoueurMatchPrive(
                organisateurMatricule,
                input.getJoueurMatricule(),
                input.getMatchId()
        );
    }

    @PutMapping("/{id}/payer")
    public Reservation payerReservation(@PathVariable Long id){
        Membre currentUser = currentUserService.getCurrentUser();
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ForbiddenException("Reservation introuvable"));

        if (!currentUserService.isAdmin(currentUser)
                && !currentUser.getMatricule().equals(reservation.getMembre().getMatricule())) {
            throw new ForbiddenException("Acces refuse");
        }

        return reservationService.payerReservation(id);
    }

    @GetMapping("/membre/{matricule}")
    public List<ReservationReponseDTO> getReservationsByMembre(@PathVariable String matricule) {
        Membre currentUser = currentUserService.getCurrentUser();

        if (!currentUserService.isAdmin(currentUser)
                && !currentUser.getMatricule().equals(matricule.trim().toUpperCase())) {
            throw new ForbiddenException("Acces refuse");
        }

        return reservationService.getReservationsByMembre(matricule);
    }
}
