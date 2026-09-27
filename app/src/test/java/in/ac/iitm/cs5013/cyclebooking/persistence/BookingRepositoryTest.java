package in.ac.iitm.cs5013.cyclebooking.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Persistence Module test: a Booking round-trips with its associations intact. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savedBookingRoundTripsWithResidentAndCycle() {
        Resident resident = entityManager.persist(
                new Resident("CE24B001", "Aditya Gautam", "Jamuna", "ce24b001@smail.iitm.ac.in", "hash"));
        Cycle cycle = entityManager.persist(new Cycle("JAM-07"));
        Instant createdAt = Instant.parse("2026-09-27T09:00:00Z");

        Long id = bookings.save(new Booking(resident, cycle, createdAt)).getId();
        entityManager.flush();
        entityManager.clear();

        Booking found = bookings.findById(id).orElseThrow();
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(found.getResident().getRollNumber()).isEqualTo("CE24B001");
        assertThat(found.getCycle().getLabel()).isEqualTo("JAM-07");
        assertThat(found.getReturnedAt()).isNull();
    }

    @Test
    void openBookingIsFoundUntilItIsReturned() {
        Resident resident = entityManager.persist(
                new Resident("CE24B009", "Test Resident", "Jamuna", "ce24b009@smail.iitm.ac.in", "hash"));
        Cycle cycle = entityManager.persist(new Cycle("JAM-08"));
        Booking booking = bookings.save(new Booking(resident, cycle, Instant.parse("2026-09-27T09:00:00Z")));
        entityManager.flush();

        assertThat(bookings.findOpenBookingForResident(resident.getId())).isPresent();

        booking.setReturnedAt(Instant.parse("2026-09-27T11:00:00Z"));
        bookings.save(booking);
        entityManager.flush();

        assertThat(bookings.findOpenBookingForResident(resident.getId())).isEmpty();
    }

    /** The 24-hour hold rule the Rule Engine will enforce in week 2. */
    @Test
    void bookingIsOverdueOnlyAfterTwentyFourHoursOfUnreturnedUse() {
        Resident resident = entityManager.persist(
                new Resident("CE24B010", "Test Resident", "Jamuna", "ce24b010@smail.iitm.ac.in", "hash"));
        Cycle cycle = entityManager.persist(new Cycle("JAM-09"));
        Instant pickedUp = Instant.parse("2026-09-27T09:00:00Z");

        Booking booking = new Booking(resident, cycle, pickedUp);
        booking.setPickedUpAt(pickedUp);

        assertThat(booking.isOverdue(pickedUp.plus(Duration.ofHours(23)))).isFalse();
        assertThat(booking.isOverdue(pickedUp.plus(Duration.ofHours(25)))).isTrue();

        booking.setReturnedAt(pickedUp.plus(Duration.ofHours(26)));
        assertThat(booking.isOverdue(pickedUp.plus(Duration.ofHours(27)))).isFalse();
    }
}
