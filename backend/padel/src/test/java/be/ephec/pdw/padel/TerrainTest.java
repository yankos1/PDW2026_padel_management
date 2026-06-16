package be.ephec.pdw.padel;

import be.ephec.pdw.padel.dto.TerrainDTO;
import be.ephec.pdw.padel.service.TerrainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TerrainTest {
    @Autowired
    TerrainService terrainService;

    // TODO [IMPORTANT] Isoler ce test avec un profil test et une base controlee.
    @Test
    void shouldReturnAvailableTerrains() {
        LocalDate date = LocalDate.of(2026, 5, 22);

        List<TerrainDTO> result = terrainService.getTerrainsDisponibles(date);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

}
