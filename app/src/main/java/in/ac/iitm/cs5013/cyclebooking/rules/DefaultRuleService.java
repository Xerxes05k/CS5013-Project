package in.ac.iitm.cs5013.cyclebooking.rules;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules from the design doc: at most 24 hours held per rental, at most 3 rentals
 * per rolling 7 days. One open rental at a time follows from the first rule: a
 * resident still holding a cycle has not finished that rental yet.
 */
@Service
class DefaultRuleService implements RuleService {

    static final int MAX_BOOKINGS_PER_WEEK = 3;
    static final Duration ROLLING_WEEK = Duration.ofDays(7);

    private final BookingRepository bookings;
    private final Clock clock;

    DefaultRuleService(BookingRepository bookings, Clock clock) {
        this.bookings = bookings;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public EligibilityResult checkEligibility(long residentId) {
        Instant now = clock.instant();

        // Overdue is checked before "already holding" so the resident sees the more
        // urgent reason.
        Optional<Booking> open = bookings.findOpenBookingForResident(residentId);
        if (open.isPresent()) {
            return open.get().isOverdue(now)
                    ? EligibilityResult.deny(EligibilityResult.Reason.OVERDUE_RETURN)
                    : EligibilityResult.deny(EligibilityResult.Reason.ALREADY_HOLDING);
        }

        long recent = bookings.countByResidentIdAndCreatedAtAfter(residentId, now.minus(ROLLING_WEEK));
        if (recent >= MAX_BOOKINGS_PER_WEEK) {
            return EligibilityResult.deny(EligibilityResult.Reason.WEEKLY_LIMIT);
        }
        return EligibilityResult.allow();
    }
}
