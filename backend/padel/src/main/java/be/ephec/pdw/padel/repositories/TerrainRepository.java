package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.JourFermeture;
import be.ephec.pdw.padel.model.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerrainRepository extends JpaRepository<Terrain,Long> {
}
