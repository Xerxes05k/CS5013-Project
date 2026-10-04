package in.ac.iitm.cs5013.cyclebooking.transaction;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The passcode is the resident's login passcode, checked against the same BCrypt
 * hash. Typing it in front of the guard is what ties the person at the stand to the
 * account that booked; the guard only has to watch it being accepted.
 */
@Service
class DefaultTransactionService implements TransactionService {

    private final BookingRepository bookings;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    DefaultTransactionService(BookingRepository bookings, PasswordEncoder passwordEncoder, Clock clock) {
        this.bookings = bookings;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CycleState confirmPickup(long bookingId, String passcode) {
        Booking booking = find(bookingId);
        Cycle cycle = booking.getCycle();
        if (booking.getPickedUpAt() != null || cycle.getState() != CycleState.BOOKED) {
            throw new TransactionRejectedException("This booking has already been picked up.");
        }
        checkPasscode(booking, passcode);

        booking.setPickedUpAt(clock.instant());
        cycle.setState(CycleState.ISSUED);
        return cycle.getState();
    }

    @Override
    @Transactional
    public CycleState confirmReturn(long bookingId, String passcode) {
        Booking booking = find(bookingId);
        Cycle cycle = booking.getCycle();
        if (booking.getPickedUpAt() == null) {
            throw new TransactionRejectedException("This cycle has not been picked up yet.");
        }
        if (booking.getReturnedAt() != null || cycle.getState() != CycleState.ISSUED) {
            throw new TransactionRejectedException("This cycle has already been returned.");
        }
        checkPasscode(booking, passcode);

        Instant now = clock.instant();
        booking.setReturnedAt(now);
        cycle.setLastReturnedAt(now);
        cycle.setState(CycleState.AVAILABLE);
        return cycle.getState();
    }

    private Booking find(long bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new TransactionRejectedException("No such booking."));
    }

    // Checked after the state checks but before any change, so a wrong passcode
    // always leaves the booking and the cycle exactly as they were.
    private void checkPasscode(Booking booking, String passcode) {
        if (passcode == null || !passwordEncoder.matches(passcode, booking.getResident().getPasscodeHash())) {
            throw new TransactionRejectedException("Wrong passcode. Nothing was changed; try again.");
        }
    }
}
