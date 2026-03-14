package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.ReservationDTO;
import be.ephec.pdw.padel.model.Reservation;
import be.ephec.pdw.padel.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;


    @PostMapping("/rejoindre")
    public Reservation rejoindre(@RequestBody ReservationDTO.PostInput input) {
        return reservationService.rejoindreMatch(input.getMatricule(), input.getMatchId());
    }

    @PutMapping("/{id}/payer")
    public Reservation payerReservation(@PathVariable Long id){
        return reservationService.payerReservation(id);
    }
}
