package in.ac.iitm.cs5013.cyclebooking.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CycleRepository extends JpaRepository<Cycle, Long> {

    List<Cycle> findByStateOrderByLabelAsc(CycleState state);

    List<Cycle> findAllByOrderByLabelAsc();

    /**
     * Moves the cycle from AVAILABLE to BOOKED in one statement and returns the number
     * of rows changed: 1 if this caller got the cycle, 0 if it was not available. This
     * is the booking lock — see DefaultBookingService.createBooking.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Cycle c set c.state = :booked, c.version = c.version + 1 "
            + "where c.id = :id and c.state = :available")
    int claimIfAvailable(@Param("id") Long id, @Param("available") CycleState available,
            @Param("booked") CycleState booked);

    default int claimIfAvailable(Long id) {
        return claimIfAvailable(id, CycleState.AVAILABLE, CycleState.BOOKED);
    }
}
