package be.ephec.pdw.padel.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class JourFermeture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate date;

    @ManyToOne
    private Site site;
}
