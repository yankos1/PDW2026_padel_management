package be.ephec.pdw.padel.dto;

public record TerrainDTO(
        long id,
        String nom,
        String heureOuverture,
        String heureFermeture
) {
}
