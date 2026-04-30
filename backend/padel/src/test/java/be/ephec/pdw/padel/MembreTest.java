package be.ephec.pdw.padel;

import be.ephec.pdw.padel.model.Membre;
import be.ephec.pdw.padel.model.MembreGlobal;
import be.ephec.pdw.padel.model.MembreLibre;
import be.ephec.pdw.padel.model.MembreSite;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MembreTest {

    @Test
    void globalMemberShouldHave21DaysDelay() {
        Membre membre = new MembreGlobal();
        assertEquals(21, membre.getDelaiReservations());
    }

    @Test
    void siteMemberShouldHave14DaysDelay() {
        Membre membre = new MembreSite();
        assertEquals(14, membre.getDelaiReservations());
    }

    @Test
    void libreMemberShouldHave5DaysDelay() {
        Membre membre = new MembreLibre();
        assertEquals(5, membre.getDelaiReservations());
    }

    @Test
    void shouldDetectActivePenalty() {
        Membre membre = new MembreGlobal();
        membre.setPenaliteActive(true);
        membre.setFinPenalite(LocalDateTime.now().plusDays(1));

        assertTrue(membre.aUnePenaliteActive());
    }

    @Test
    void shouldDetectExpiredPenalty() {
        Membre membre = new MembreGlobal();
        membre.setPenaliteActive(true);
        membre.setFinPenalite(LocalDateTime.now().minusDays(1));

        assertFalse(membre.aUnePenaliteActive());
    }
}
