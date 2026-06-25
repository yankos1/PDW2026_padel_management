package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.JoueurDTO;
import be.ephec.pdw.padel.dto.MatchDTO;
import be.ephec.pdw.padel.dto.MatchReponseDTO;
import be.ephec.pdw.padel.dto.TerrainDTO;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.repositories.SiteRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.service.MatchService;
import be.ephec.pdw.padel.service.TerrainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final TerrainService terrainService;
    private final TerrainRepository terrainRepository;
    private final SiteRepository siteRepository;

    // TODO [IMPORTANT][SECURITE] Recuperer l'organisateur depuis l'utilisateur authentifie au lieu de lire organisateur_matricule dans le DTO.
    @PostMapping
    public MatchReponseDTO createMatch(@RequestBody MatchDTO.PostInput input) {
        //return matchService.creerMatch(input.getOrganisateur_matricule(), input.getTerrainID(),input.getDate(),input.isEstPublic());
        Match match = matchService.creerMatch(
                input.getOrganisateur_matricule(),
                input.getTerrainID(),
                input.getDate(),
                input.isEstPublic()
        );

        return new MatchReponseDTO(
                match.getId(),
                match.getDateHeureDebut(),
                1, // hardcodé car tjrs 1 a la création
                match.getTerrain().getNom(),
                match.getTerrain().getSite().getId(),
                match.getTerrain().getSite().getName(),
                match.getOrganisateur().getMatricule(),
                match.isEstPublic()
        );
    }

    @GetMapping("/disponibles")
    public List<MatchReponseDTO> matchsDisponibles() {
        return matchService.matchsDisponibles();
    }

    @GetMapping("/{id}/joueurs")
    public List<JoueurDTO> joueursInscrits(@PathVariable Long id) {
        return matchService.joueursInscritMatch(id);
    }

    /************
     * Terrains
     *********/

    @GetMapping("/terrains")
    public List<TerrainDTO> getTerrains() {
        return terrainRepository.findAll()
                .stream()
                .map(t -> new TerrainDTO(t.getId(), t.getNom(), t.getSite().getHeureOuverture().toString(), t.getSite().getHeureFermeture().toString()))
                .toList();
    }

    @GetMapping("/terrains/disponibles")
    public List<TerrainDTO> getTerrainsDisponibles(
            @RequestParam LocalDate date
    ) {
        return terrainService.getTerrainsDisponibles(date);
    }
    @GetMapping("/terrains/disponibles-par-creneau")
    public List<TerrainDTO> getTerrainsDisponiblesParCreneau(
            @RequestParam LocalDate date,
            @RequestParam String heure,
            @RequestParam Long siteId
    ) {
        return terrainService.getTerrainsDisponiblesParCreneau(date, heure, siteId);
    }

    @GetMapping("/sites/{siteId}/terrains")
    public List<TerrainDTO> getTerrainsBySite(@PathVariable Long siteId) {

        return terrainRepository.findBySiteId(siteId)
                .stream()
                .map(t -> new TerrainDTO(
                        t.getId(),
                        t.getNom(),
                        t.getSite().getHeureOuverture().toString(),
                        t.getSite().getHeureFermeture().toString()
                ))
                .toList();
    }


    /************
     * Créneaux
     *********/

    @GetMapping("/creneaux-disponibles")
    public List<String> getCreneauxDisponibles(@RequestParam LocalDate date) {
        return terrainService.getCreneauxDisponibles(date);
    }

    /************
     * Sites
     *********/

    @GetMapping("/sites")
    // TODO [IMPORTANT][ARCHITECTURE] Retourner un SiteDTO pour ne pas exposer directement l'entite JPA.
    public List<Site> getSites() {
        return siteRepository.findAll();
    }

}
