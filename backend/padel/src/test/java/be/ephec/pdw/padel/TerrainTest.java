package be.ephec.pdw.padel;

import be.ephec.pdw.padel.dto.TerrainDTO;
import be.ephec.pdw.padel.service.TerrainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class TerrainTest {
    @Autowired
    TerrainService terrainService;

    @Test
    void shouldReturnAvailableTerrains() {
        LocalDate date = LocalDate.of(2026, 5, 22);

        List<TerrainDTO> result = terrainService.getTerrainsDisponibles(date);

        assertNotNull(result);
    }

}
