package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TerrainRepository extends JpaRepository<Terrain,Long> {
    List<Terrain> findBySiteId(Long siteId);

    @Query("""
            select count(t)
            from Terrain t
            where (:siteId is null or t.site.id = :siteId)
              and (:terrainId is null or t.id = :terrainId)
            """)
    long countAccessibleTerrains(@Param("siteId") Long siteId, @Param("terrainId") Long terrainId);

    @Query("""
            select t
            from Terrain t
            where (:siteId is null or t.site.id = :siteId)
            order by t.site.name, t.nom
            """)
    List<Terrain> findAccessibleTerrains(@Param("siteId") Long siteId);
}
