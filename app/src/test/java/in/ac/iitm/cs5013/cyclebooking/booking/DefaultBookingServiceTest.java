package in.ac.iitm.cs5013.cyclebooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Booking Module test. Deliberately not @Transactional: each thread must run its own
 * real transaction against SQLite, otherwise the race being tested never happens.
 */
@SpringBootTest
class DefaultBookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ResidentRepository residents;

    @Autowired
    private CycleRepository cycles;

    @Autowired
    private BookingRepository bookings;

    @AfterEach
    void cleanUp() {
        bookings.deleteAll();
        cycles.deleteAll();
        residents.deleteAll();
    }

    private Resident resident(String roll) {
        return residents.save(new Resident(roll, "Booking Test", "Jamuna",
                roll.toLowerCase() + "@smail.iitm.ac.in", "hash"));
    }

    @Test
    void createBooking_marksCycleBookedAndRecordsTheBooking() {
        Resident r = resident("CE24B060");
        Cycle c = cycles.save(new Cycle("BOOK-01"));

        BookingConfirmation done = bookingService.createBooking(r.getId(), c.getId());

        assertThat(done.cycleLabel()).isEqualTo("BOOK-01");
        assertThat(cycles.findById(c.getId()).orElseThrow().getState()).isEqualTo(CycleState.BOOKED);
        assertThat(bookings.findOpenBookingForResident(r.getId())).isPresent();
    }

    @Test
    void createBooking_rejectsCycleThatIsNotAvailable() {
        Resident r = resident("CE24B061");
        Cycle c = new Cycle("BOOK-02");
        c.setState(CycleState.UNDER_REPAIR);
        Cycle saved = cycles.save(c);

        assertThatThrownBy(() -> bookingService.createBooking(r.getId(), saved.getId()))
                .isInstanceOf(BookingRejectedException.class);
        assertThat(bookings.count()).isZero();
    }

    @Test
    void createBooking_rejectsWhenRuleEngineDenies() {
        Resident r = resident("CE24B062");
        Cycle first = cycles.save(new Cycle("BOOK-03"));
        Cycle second = cycles.save(new Cycle("BOOK-04"));
        bookingService.createBooking(r.getId(), first.getId());

        assertThatThrownBy(() -> bookingService.createBooking(r.getId(), second.getId()))
                .isInstanceOf(BookingRejectedException.class)
                .hasMessageContaining("already have a cycle");
        assertThat(cycles.findById(second.getId()).orElseThrow().getState()).isEqualTo(CycleState.AVAILABLE);
    }

    @Test
    void createBooking_rejectsSecondConcurrentBookingOfSameCycle() throws Exception {
        Resident a = resident("CE24B063");
        Resident b = resident("CE24B064");
        long cycleId = cycles.save(new Cycle("BOOK-05")).getId();

        // Both threads wait at the latch, then call createBooking at the same moment.
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<BookingConfirmation>> results = new ArrayList<>();
        for (Resident r : List.of(a, b)) {
            Callable<BookingConfirmation> attempt = () -> {
                start.await();
                return bookingService.createBooking(r.getId(), cycleId);
            };
            results.add(pool.submit(attempt));
        }
        start.countDown();
        pool.shutdown();

        int succeeded = 0;
        int rejected = 0;
        for (Future<BookingConfirmation> f : results) {
            try {
                f.get();
                succeeded++;
            } catch (ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(BookingRejectedException.class);
                rejected++;
            }
        }

        assertThat(succeeded).isEqualTo(1);
        assertThat(rejected).isEqualTo(1);
        assertThat(bookings.count()).isEqualTo(1);
        assertThat(cycles.findById(cycleId).orElseThrow().getState()).isEqualTo(CycleState.BOOKED);
    }
}
