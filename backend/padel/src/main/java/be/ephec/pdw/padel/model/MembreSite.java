package be.ephec.pdw.padel.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("SITE")
@Getter
@Setter
public class MembreSite extends Membre {
    @ManyToOne
    private Site site;

    @Override
    public long getDelaiReservations() {
        return 14;
    }
}
