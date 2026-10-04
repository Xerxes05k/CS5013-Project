package in.ac.iitm.cs5013.cyclebooking.booking;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import in.ac.iitm.cs5013.cyclebooking.rules.EligibilityResult;
import in.ac.iitm.cs5013.cyclebooking.rules.RuleService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultBookingService implements BookingService {

    private final CycleRepository cycles;
    private final ResidentRepository residents;
    private final BookingRepository bookings;
    private final RuleService rules;
    private final Clock clock;

    DefaultBookingService(CycleRepository cycles, ResidentRepository residents, BookingRepository bookings,
            RuleService rules, Clock clock) {
        this.cycles = cycles;
        this.residents = residents;
        this.bookings = bookings;
        this.rules = rules;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CycleStatus> getAvailability() {
        return cycles.findAllByOrderByLabelAsc().stream()
                .map(c -> new CycleStatus(c.getId(), c.getLabel(), c.getState()))
                .toList();
    }

    @Override
    @Transactional
    public BookingConfirmation createBooking(long residentId, long cycleId) {
        EligibilityResult eligibility = rules.checkEligibility(residentId);
        if (!eligibility.allowed()) {
            throw new BookingRejectedException(eligibility.reason().message());
        }

        // The lock: a single conditional UPDATE flips the cycle AVAILABLE -> BOOKED.
        // The database runs it atomically, so when two residents race for the same
        // cycle exactly one UPDATE matches the row and the other changes nothing.
        if (cycles.claimIfAvailable(cycleId) == 0) {
            throw new BookingRejectedException("That cycle is no longer available. Pick another.");
        }

        Cycle cycle = cycles.findById(cycleId).orElseThrow();
        Instant now = clock.instant();
        Booking booking = bookings.save(new Booking(residents.getReferenceById(residentId), cycle, now));
        return new BookingConfirmation(booking.getId(), cycle.getLabel(), now);
    }
}
