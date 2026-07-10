package be.ephec.pdw.padel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class ReservationDTO {
    @Data
    public static class PostInput {
        @NotNull
        Long matchId;
    }

    @Data
    public static class AjoutJoueurPriveInput {
        @NotBlank
        String joueurMatricule;

        @NotNull
        Long matchId;
    }
}
