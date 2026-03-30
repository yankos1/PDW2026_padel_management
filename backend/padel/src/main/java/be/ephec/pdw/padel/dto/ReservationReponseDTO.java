package be.ephec.pdw.padel.dto;

public record ReservationReponseDTO(
        Long id,
        MatchReponseDTO match,
        boolean paye
) {
}
