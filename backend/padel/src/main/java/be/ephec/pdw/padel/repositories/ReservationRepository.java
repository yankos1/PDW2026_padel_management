package be.ephec.pdw.padel.repositories;

import be.ephec.pdw.padel.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
}
