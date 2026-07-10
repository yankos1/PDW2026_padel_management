package be.ephec.pdw.padel.model;

import be.ephec.pdw.padel.constants.BusinessConstants;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("LIBRE")
public class MembreLibre extends Membre {
    @Override
    public long getDelaiReservations() {
        return BusinessConstants.LIBRE_MEMBER_RESERVATION_DAYS;
    }
}
