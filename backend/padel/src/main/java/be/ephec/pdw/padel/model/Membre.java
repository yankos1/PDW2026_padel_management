package be.ephec.pdw.padel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_membre")
@Data
public abstract class Membre {
    @Id
    @Column(nullable = false, unique = true)
    private String matricule; // Gxxxx / Sxxxx / Lxxxx
    private String nom;
    private String prenom;
    private String email;

    private double soldeDu;

    private boolean penaliteActive;
    private LocalDateTime finPenalite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private Role role;

    @JsonIgnore
    private String adminPasswordHash;

    public boolean aUnePenaliteActive() {
        return penaliteActive && finPenalite != null  && finPenalite.isAfter(LocalDateTime.now());
    }

    public boolean isPenaliteActive() {
        return aUnePenaliteActive();
    }

    public abstract long getDelaiReservations();
}
