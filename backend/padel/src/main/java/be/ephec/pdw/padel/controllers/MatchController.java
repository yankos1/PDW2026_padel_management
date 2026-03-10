package be.ephec.pdw.padel.controllers;

import be.ephec.pdw.padel.dto.MatchDTO;
import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/creer")
    public Match createMatch(@RequestBody MatchDTO.PostInput input) {

        return matchService.creerMatch(input.getOrganisateur_matricule(), input.getTerrainID(),input.getDate());
    }


}
