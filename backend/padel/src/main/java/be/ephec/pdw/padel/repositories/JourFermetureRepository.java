package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.JourFermeture;
import be.ephec.pdw.padel.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface JourFermetureRepository extends JpaRepository<JourFermeture,Long> {
    boolean existsBySiteIsNullAndDate(LocalDate date);

    boolean existsBySiteAndDate(Site site, LocalDate date);

    Site site(Site site);
}
