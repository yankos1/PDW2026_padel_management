package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Match;
import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
    long countByMatch(Match match);

    long countByMatchAndEstPayeeTrue(Match match);

    long countByMatchAndEstPayeeFalse(Match match);

    boolean existsByMatchAndMembre(Match match, Membre membre);

    boolean existsByMatchAndMembreAndEstPayeeFalse(Match match, Membre membre);

    long countByEstPayeeTrue();

    Long id(Long id);

    List<Reservation> findByMembre(Membre membre);

    void deleteByMatchAndEstPayeeFalse(Match match);
}
