package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.StatutMatch;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.MembreRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MembreRepository membreRepository;
    private final TerrainRepository terrainRepository;
    private final MatchRepository matchRepository;


    public Match creerMatch(String matricule, Long idTerrain, LocalDateTime dateHeure){

        Membre membre = membreRepository.findById(matricule).orElseThrow(() -> new RuntimeException("Membre n'existe pas"));

        if (membre.aUnePenaliteActive()) {
            throw new RuntimeException("Le membre a une pénalité");
        }

        Terrain terrain = terrainRepository.findById(idTerrain).orElseThrow(() -> new RuntimeException("Terrain introuvable"));

        Match match = Match.builder()
                .organisateur(membre)
                .terrain(terrain)
                .dateHeureDebut(dateHeure)
                .statut(StatutMatch.PLANIFIE)
                .build();

        return matchRepository.save(match);

    }
}
