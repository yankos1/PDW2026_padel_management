package be.ephec.pdw.padel.model;

import jakarta.persistence.*;

@Entity
public class Terrain {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;

    @ManyToOne
    private Site site;
}
