package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site,Long> {
}
