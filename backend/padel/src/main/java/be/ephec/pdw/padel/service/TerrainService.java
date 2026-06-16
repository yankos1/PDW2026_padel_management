package be.ephec.pdw.padel.service;

import be.ephec.pdw.padel.configuration.BusinessRuleException;
import be.ephec.pdw.padel.dto.TerrainDTO;
import be.ephec.pdw.padel.model.Site;
import be.ephec.pdw.padel.model.Terrain;
import be.ephec.pdw.padel.repositories.JourFermetureRepository;
import be.ephec.pdw.padel.repositories.MatchRepository;
import be.ephec.pdw.padel.repositories.TerrainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TerrainService {
    private final JourFermetureRepository jourFermetureRepository;
    private final TerrainRepository terrainRepository;
    private final MatchRepository matchRepository;


    public void verifierJourFermeture(Site site, LocalDateTime dateMatch){
        LocalDate date = dateMatch.toLocalDate();

        // TODO [IMPORTANT] Distinguer fermeture globale et fermeture par site dans le modele.
        if(jourFermetureRepository.existsByDate(date)){
            throw new BusinessRuleException("Les sites sont fermé ce jour");
        }

        if (jourFermetureRepository.existsBySiteAndDate(site, date)){
            throw new BusinessRuleException("Ce site est fermé ce jour");

        }
    }

    public List<TerrainDTO> getTerrainsDisponibles(LocalDate date) {
        // vérifier jours de fermeture
        if (jourFermetureRepository.existsByDate(date)) {
            return List.of();
        }

        // récupérer tous les terrains
        // TODO [IMPORTANT] Filtrer aussi par site pour eviter des disponibilites incoherentes.
        List<Terrain> terrains = terrainRepository.findAll();

        // filtrer ceux qui ont au moins un créneau libre
        return terrains.stream()
                .filter(t -> hasAvailableSlot(t, date))
                .map(this::toDTO)
                .toList();
    }

    public List<TerrainDTO> getTerrainsDisponiblesParCreneau(LocalDate date, String heure, Long siteId) {

        LocalTime time = LocalTime.parse(heure);

        return terrainRepository.findBySiteId(siteId).stream()
                .filter(t -> !matchRepository.existsByTerrainAndDateHeureDebut(
                        t,
                        LocalDateTime.of(date, time)
                ))
                .map(this::toDTO)
                .toList();
    }

    private boolean hasAvailableSlot(Terrain terrain, LocalDate date) {
        // slot disponible par jour


        LocalTime open = terrain.getSite().getHeureOuverture();
        LocalTime close = terrain.getSite().getHeureFermeture();

        LocalTime current = open;

        while (true) {
            LocalTime end = current.plusMinutes(90);

            if (end.isAfter(close)) break;

            boolean exists = matchRepository.existsByTerrainAndDateHeureDebut(
                    terrain,
                    LocalDateTime.of(date, current)
            );

            if (!exists) return true;

            current = current.plusMinutes(105); // 90 + 15
        }

        return false;
    }

    private TerrainDTO toDTO(Terrain t) {
        return new TerrainDTO(
                t.getId(),
                t.getNom(),
                t.getSite().getHeureOuverture().toString(),
                t.getSite().getHeureFermeture().toString()
        );
    }

    public List<String> getCreneauxDisponibles(LocalDate date) {

        if (jourFermetureRepository.existsByDate(date)) {
            return List.of();
        }

        List<Terrain> terrains = terrainRepository.findAll();
        List<String> slots = new ArrayList<>();

        for (Terrain terrain : terrains) {

            LocalTime open = terrain.getSite().getHeureOuverture();
            LocalTime close = terrain.getSite().getHeureFermeture();

            LocalTime current = open;

            while (true) {
                LocalTime end = current.plusMinutes(90);

                if (end.isAfter(close)) break;

                boolean exists = matchRepository.existsByTerrainAndDateHeureDebut(
                        terrain,
                        LocalDateTime.of(date, current)
                );

                if (!exists) {
                    slots.add(current.toString());
                }

                current = current.plusMinutes(105);
            }
        }

        return slots.stream()
                .distinct()
                .sorted()
                .toList();
    }
}
