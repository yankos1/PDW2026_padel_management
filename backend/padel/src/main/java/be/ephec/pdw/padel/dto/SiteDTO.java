package be.ephec.pdw.padel.dto;

import be.ephec.pdw.padel.model.Site;

public record SiteDTO(
        Long id,
        String name,
        String heureOuverture,
        String heureFermeture
) {
    public static SiteDTO from(Site site) {
        return new SiteDTO(
                site.getId(),
                site.getName(),
                site.getHeureOuverture().toString(),
                site.getHeureFermeture().toString()
        );
    }
}
