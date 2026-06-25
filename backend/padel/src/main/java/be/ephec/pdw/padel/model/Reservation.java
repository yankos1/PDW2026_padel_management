package be.ephec.pdw.padel.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
// TODO [IMPORTANT][ARCHITECTURE] Ajouter une contrainte unique sur le couple match/membre.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Membre membre;

    @ManyToOne
    @JsonBackReference
    private Match match;

    private LocalDateTime dateReservation;
    private LocalDateTime datePaiement;
    private double montant; // total 60euros = 15 par joueurs
    private boolean estPayee;

    @Enumerated(EnumType.STRING)
    private StatutReservation statut;

}
