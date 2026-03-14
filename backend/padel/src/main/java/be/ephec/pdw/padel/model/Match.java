package be.ephec.pdw.padel.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "match_padel") // mot sql
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime dateHeureDebut;

    private boolean estPublic;

    @ManyToOne
    @JoinColumn (name = "terrain_id")
    private Terrain terrain;

    @ManyToOne
    @JoinColumn(name = "organisateur_matricule")
    private Membre organisateur;

    @Enumerated(EnumType.STRING)
    private StatutMatch statut;

    @OneToMany(mappedBy = "match")
    @JsonManagedReference
    private List<Reservation> reservations;

}

