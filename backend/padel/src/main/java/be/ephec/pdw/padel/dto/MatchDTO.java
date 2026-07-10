package be.ephec.pdw.padel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class MatchDTO {

    @Data
    @AllArgsConstructor
    @Builder
    public static class PostInput{
        @NotNull
        Long terrainID;

        @NotNull
        LocalDateTime date;

        boolean estPublic;
    }
}
