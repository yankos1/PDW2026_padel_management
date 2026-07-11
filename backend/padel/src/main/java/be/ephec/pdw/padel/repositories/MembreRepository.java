package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Membre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre,String> {

    Optional<Membre> findTopByMatriculeStartingWithOrderByMatriculeDesc(String prefix);

    // Optional<Membre> findByMatricule(String organisateur_matricule);

    @Query("select coalesce(sum(m.soldeDu), 0) from Membre m")
    Double sumAllSoldesDus();

    @Query("select coalesce(sum(ms.soldeDu), 0) from MembreSite ms where ms.site.id = :siteId")
    Double sumSoldesDusBySite(@Param("siteId") Long siteId);

    @Query("select count(m) from Membre m")
    long countAllMembers();

    @Query("select count(ms) from MembreSite ms where ms.site.id = :siteId")
    long countMembersBySite(@Param("siteId") Long siteId);
}
