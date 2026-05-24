package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre,String> {

    Optional<Membre> findTopByMatriculeStartingWithOrderByMatriculeDesc(String prefix);

    // Optional<Membre> findByMatricule(String organisateur_matricule);
}
