package be.ephec.pdw.padel.dto;

import lombok.Data;

public class ReservationDTO {
    @Data
    public static class PostInput {
        String matricule;
        Long matchId;
    }
}
