package be.ephec.pdw.padel.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("SITE")
public class MembreSite extends Membre {
    @ManyToOne
    private Site site;
}
