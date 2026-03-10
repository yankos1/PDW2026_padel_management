package be.ephec.pdw.padel.dto;

import be.ephec.pdw.padel.model.StatutMatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class MatchDTO {

    @Data
    @AllArgsConstructor
    @Builder
    public static class PostInput{
        Long id;
        String organisateur_matricule;
        Long terrainID;
        LocalDateTime date;
        StatutMatch statut;
    }
}
