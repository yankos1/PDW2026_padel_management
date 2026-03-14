package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.JoueurDTO;
import be.ephec.pdw.padel.dto.MatchDTO;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public Match createMatch(@RequestBody MatchDTO.PostInput input) {
        return matchService.creerMatch(input.getOrganisateur_matricule(), input.getTerrainID(),input.getDate(),input.isEstPublic());
    }

    @GetMapping("/disponibles")
    public List<Match> matchsDisponibles(){
        return matchService.matchsDisponibles();
    }

    @GetMapping("/{id}/joueurs")
    public List<JoueurDTO> joueursInscrits(@PathVariable Long id){
        return matchService.joueursInscritMatch(id);
    }


}
