package be.ephec.pdw.padel.dto;

import be.ephec.pdw.padel.model.StatutReservation;

public record ReservationReponseDTO(
        Long id,
        MatchReponseDTO match,
        boolean paye,
        boolean estPayee,
        StatutReservation statut
) {
}
