package be.ephec.pdw.padel.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Membre membre;

    @ManyToOne
    private Match match;

    private LocalDateTime dateReservation;
    private LocalDateTime datePaiement;
    private double montant; // total 60euros = 15 par joueurs
    private boolean estPayee;

    @Enumerated(EnumType.STRING)
    private StatutReservation statut;

}
