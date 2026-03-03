package be.ephec.pdw.padel.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "match_padel") // mot sql
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int  id;
    private LocalDateTime dateHeureDebut;

    private boolean esPublic;

    @ManyToOne
    private Terrain terrain;

    @ManyToOne
    private Membre organisateur;

    @Enumerated(EnumType.STRING)
    private StatutMatch statut;

    @OneToMany(mappedBy = "match")
    private List<Reservation> reservations;

}

