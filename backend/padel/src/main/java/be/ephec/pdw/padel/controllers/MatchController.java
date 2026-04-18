package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.JoueurDTO;
import be.ephec.pdw.padel.dto.MatchDTO;
import be.ephec.pdw.padel.dto.MatchReponseDTO;
import be.ephec.pdw.padel.dto.TerrainDTO;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import be.ephec.pdw.padel.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final TerrainRepository terrainRepository;

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
                match.isEstPublic()
        );
    }

    @GetMapping("/disponibles")
    public List<MatchReponseDTO> matchsDisponibles(){
        return matchService.matchsDisponibles();
    }

    @GetMapping("/{id}/joueurs")
    public List<JoueurDTO> joueursInscrits(@PathVariable Long id){
        return matchService.joueursInscritMatch(id);
    }

    @GetMapping("/terrains")
    public List<TerrainDTO> getTerrains(){
        return terrainRepository.findAll()
                .stream()
                .map(t -> new TerrainDTO(t.getId(),t.getNom()))
                .toList();
    }


}
