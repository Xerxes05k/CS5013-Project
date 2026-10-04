package in.ac.iitm.cs5013.cyclebooking.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.IdCheck;
import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pickup/Return test: the state transitions BOOKED -> ISSUED -> AVAILABLE, and that a
 * wrong passcode or an out-of-order step changes nothing.
 */
@SpringBootTest
class DefaultTransactionServiceTest {

    private static final String PASSCODE = "cycle1234";

    @Autowired
    private TransactionService transactions;

    @Autowired
    private ResidentRepository residents;

    @Autowired
    private CycleRepository cycles;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private long bookingId;
    private long cycleId;

    /** A fresh booking in the state createBooking leaves it: cycle BOOKED, not picked up. */
    @BeforeEach
    void bookACycle() {
        Resident r = residents.save(new Resident("CE24B070", "Txn Test", "Jamuna",
                "ce24b070@smail.iitm.ac.in", passwordEncoder.encode(PASSCODE)));
        Cycle c = new Cycle("TXN-01");
        c.setState(CycleState.BOOKED);
        c = cycles.save(c);
        cycleId = c.getId();
        bookingId = bookings.save(new Booking(r, c, Instant.now(), IdCheck.CARD_SCAN)).getId();
    }

    @AfterEach
    void cleanUp() {
        bookings.deleteAll();
        cycles.deleteAll();
        residents.deleteAll();
    }

    private CycleState cycleState() {
        return cycles.findById(cycleId).orElseThrow().getState();
    }

    private Booking booking() {
        return bookings.findById(bookingId).orElseThrow();
    }

    @Test
    void confirmPickup_withWrongPasscodeChangesNothing() {
        assertThatThrownBy(() -> transactions.confirmPickup(bookingId, "wrong"))
                .isInstanceOf(TransactionRejectedException.class)
                .hasMessageContaining("Wrong passcode");

        assertThat(cycleState()).isEqualTo(CycleState.BOOKED);
        assertThat(booking().getPickedUpAt()).isNull();
    }

    @Test
    void confirmPickup_withCorrectPasscodeMovesBookedToIssued() {
        CycleState state = transactions.confirmPickup(bookingId, PASSCODE);

        assertThat(state).isEqualTo(CycleState.ISSUED);
        assertThat(cycleState()).isEqualTo(CycleState.ISSUED);
        assertThat(booking().getPickedUpAt()).isNotNull();
    }

    @Test
    void confirmPickup_twiceIsRefused() {
        transactions.confirmPickup(bookingId, PASSCODE);

        assertThatThrownBy(() -> transactions.confirmPickup(bookingId, PASSCODE))
                .isInstanceOf(TransactionRejectedException.class);
        assertThat(cycleState()).isEqualTo(CycleState.ISSUED);
    }

    @Test
    void confirmReturn_beforePickupIsRefused() {
        assertThatThrownBy(() -> transactions.confirmReturn(bookingId, PASSCODE))
                .isInstanceOf(TransactionRejectedException.class)
                .hasMessageContaining("not been picked up");

        assertThat(cycleState()).isEqualTo(CycleState.BOOKED);
        assertThat(booking().getReturnedAt()).isNull();
    }

    @Test
    void confirmReturn_withWrongPasscodeLeavesCycleIssued() {
        transactions.confirmPickup(bookingId, PASSCODE);

        assertThatThrownBy(() -> transactions.confirmReturn(bookingId, "wrong"))
                .isInstanceOf(TransactionRejectedException.class);

        assertThat(cycleState()).isEqualTo(CycleState.ISSUED);
        assertThat(booking().getReturnedAt()).isNull();
    }

    @Test
    void confirmReturn_withCorrectPasscodeMakesCycleAvailableAgain() {
        transactions.confirmPickup(bookingId, PASSCODE);

        CycleState state = transactions.confirmReturn(bookingId, PASSCODE);

        assertThat(state).isEqualTo(CycleState.AVAILABLE);
        Cycle cycle = cycles.findById(cycleId).orElseThrow();
        assertThat(cycle.getState()).isEqualTo(CycleState.AVAILABLE);
        assertThat(cycle.getLastReturnedAt()).isNotNull();
        assertThat(booking().getReturnedAt()).isEqualTo(cycle.getLastReturnedAt());
    }
}
