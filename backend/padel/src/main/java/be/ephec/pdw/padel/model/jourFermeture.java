package be.ephec.pdw.padel.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class jourFermeture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private LocalDate date;

    @ManyToOne
    private Site site;
}
