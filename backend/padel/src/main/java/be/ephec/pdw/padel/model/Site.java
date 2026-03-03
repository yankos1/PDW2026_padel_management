package be.ephec.pdw.padel.model;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.List;

@Entity
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;

    @OneToMany(mappedBy = "site")
    private List<Terrain> terrains;
    @OneToMany(mappedBy = "site")
    private List<jourFermeture> jourFermeture;

}
