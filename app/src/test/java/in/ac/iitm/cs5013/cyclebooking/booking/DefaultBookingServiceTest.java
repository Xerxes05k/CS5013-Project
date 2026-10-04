package in.ac.iitm.cs5013.cyclebooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.idverify.ExtractedIdentity;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.IdCheck;
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

    private static ResidentIdentity identity(Resident r) {
        return new ResidentIdentity(r.getId(), r.getRollNumber(), r.getSmailAddress());
    }

    /** What a clean scan of this resident's own card returns. */
    private static ExtractedIdentity ownCard(Resident r) {
        return new ExtractedIdentity(r.getName(), r.getRollNumber(), r.getHostel(), 91f);
    }

    @Test
    void createBooking_marksCycleBookedAndRecordsTheBooking() {
        Resident r = resident("CE24B060");
        Cycle c = cycles.save(new Cycle("BOOK-01"));

        BookingConfirmation done = bookingService.createBooking(identity(r), c.getId(), ownCard(r));

        assertThat(done.cycleLabel()).isEqualTo("BOOK-01");
        assertThat(cycles.findById(c.getId()).orElseThrow().getState()).isEqualTo(CycleState.BOOKED);
        assertThat(bookings.findOpenBookingForResident(r.getId()).orElseThrow().getIdCheck())
                .isEqualTo(IdCheck.CARD_SCAN);
    }

    @Test
    void createBooking_rejectsSomeoneElsesCardAndLeavesCycleAvailable() {
        Resident r = resident("CE24B065");
        Cycle c = cycles.save(new Cycle("BOOK-06"));
        ExtractedIdentity otherCard = new ExtractedIdentity("Someone Else", "ME23B045", "Jamuna", 92f);

        assertThatThrownBy(() -> bookingService.createBooking(identity(r), c.getId(), otherCard))
                .isInstanceOf(IdCheckFailedException.class)
                .hasMessageContaining("different roll number");
        assertThat(cycles.findById(c.getId()).orElseThrow().getState()).isEqualTo(CycleState.AVAILABLE);
        assertThat(bookings.count()).isZero();
    }

    @Test
    void createBooking_rejectsOwnRollNumberReadWithLowConfidence() {
        Resident r = resident("CE24B066");
        Cycle c = cycles.save(new Cycle("BOOK-07"));
        ExtractedIdentity blurry = new ExtractedIdentity(null, r.getRollNumber(), null, 40f);

        assertThatThrownBy(() -> bookingService.createBooking(identity(r), c.getId(), blurry))
                .isInstanceOf(IdCheckFailedException.class)
                .hasMessageContaining("Retake the photo");
        assertThat(bookings.count()).isZero();
    }

    @Test
    void createBookingWithTypedRoll_booksAndMarksBookingForGuardCardCheck() {
        Resident r = resident("CE24B067");
        Cycle c = cycles.save(new Cycle("BOOK-08"));

        bookingService.createBookingWithTypedRoll(identity(r), c.getId(), " ce24b067 ");

        assertThat(bookings.findOpenBookingForResident(r.getId()).orElseThrow().getIdCheck())
                .isEqualTo(IdCheck.TYPED_ROLL);
        assertThat(bookingService.currentRental(r.getId()).orElseThrow().guardShouldCheckCard()).isTrue();
    }

    @Test
    void createBookingWithTypedRoll_rejectsAnotherResidentsRollNumber() {
        Resident r = resident("CE24B068");
        Cycle c = cycles.save(new Cycle("BOOK-09"));

        assertThatThrownBy(() -> bookingService.createBookingWithTypedRoll(identity(r), c.getId(), "CE24B001"))
                .isInstanceOf(IdCheckFailedException.class);
        assertThat(cycles.findById(c.getId()).orElseThrow().getState()).isEqualTo(CycleState.AVAILABLE);
    }

    @Test
    void createBooking_rejectsCycleThatIsNotAvailable() {
        Resident r = resident("CE24B061");
        Cycle c = new Cycle("BOOK-02");
        c.setState(CycleState.UNDER_REPAIR);
        Cycle saved = cycles.save(c);

        assertThatThrownBy(() -> bookingService.createBooking(identity(r), saved.getId(), ownCard(r)))
                .isInstanceOf(BookingRejectedException.class);
        assertThat(bookings.count()).isZero();
    }

    @Test
    void createBooking_rejectsWhenRuleEngineDenies() {
        Resident r = resident("CE24B062");
        Cycle first = cycles.save(new Cycle("BOOK-03"));
        Cycle second = cycles.save(new Cycle("BOOK-04"));
        bookingService.createBooking(identity(r), first.getId(), ownCard(r));

        assertThatThrownBy(() -> bookingService.createBooking(identity(r), second.getId(), ownCard(r)))
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
                return bookingService.createBooking(identity(r), cycleId, ownCard(r));
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
