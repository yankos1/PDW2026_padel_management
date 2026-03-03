package be.ephec.pdw.padel.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_membre")
public abstract class Membre {
    @Id
    @Column(nullable = false, unique = true)
    private String matricule; // Gxxxx / Sxxxx / Lxxxx
    private String nom;
    private String prenom;
    private String email;

    private boolean penaliteActive;
    private LocalDateTime finPenalite;

}
