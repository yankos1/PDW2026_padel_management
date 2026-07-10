package be.ephec.pdw.padel.model;

import be.ephec.pdw.padel.constants.BusinessConstants;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("GLOBAL")
public class MembreGlobal extends Membre {
    @Override
    public long getDelaiReservations() {
        return BusinessConstants.GLOBAL_MEMBER_RESERVATION_DAYS;
    }
}
