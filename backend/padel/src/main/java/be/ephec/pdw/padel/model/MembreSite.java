package be.ephec.pdw.padel.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
@DiscriminatorValue("SITE")
public class MembreSite extends Membre {
    @ManyToOne
    private Site site;

    @Override
    public long getDelaiReservations() {
        return 14;
    }
}
