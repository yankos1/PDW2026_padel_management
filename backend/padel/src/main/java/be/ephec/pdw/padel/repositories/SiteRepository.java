package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site,Long> {
    @Query("""
            select s
            from Site s
            where (:siteId is null or s.id = :siteId)
            """)
    List<Site> findSitesForDashboard(@Param("siteId") Long siteId);
}
