package be.ephec.pdw.padel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class MatchDTO {

    @Data
    @AllArgsConstructor
    @Builder
    public static class PostInput{
        String organisateur_matricule;
        Long terrainID;
        LocalDateTime date;
        boolean estPublic;
    }
}
