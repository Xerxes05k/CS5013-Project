package in.ac.iitm.cs5013.cyclebooking.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Backs the 3-rentals-per-rolling-week rule (Rule Engine, week 2). */
    long countByResidentIdAndCreatedAtAfter(Long residentId, Instant since);

    /** A resident's open rental, if any: booked or picked up but not yet returned. */
    @Query("select b from Booking b where b.resident.id = :residentId and b.returnedAt is null")
    Optional<Booking> findOpenBookingForResident(@Param("residentId") Long residentId);

    @Query("select b from Booking b where b.pickedUpAt is not null and b.returnedAt is null")
    List<Booking> findAllOutstanding();

    List<Booking> findByResidentIdOrderByCreatedAtDesc(Long residentId);
}
