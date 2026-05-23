package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerrainRepository extends JpaRepository<Terrain,Long> {
    List<Terrain> findBySiteId(Long siteId);
}
