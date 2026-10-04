package in.ac.iitm.cs5013.cyclebooking.reporting;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.reporting.UsageReport.CycleUsage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usage is the time between pickup and return, the hours the cycle was actually out
 * of the stand. Time spent booked but not yet picked up is not usage. A cycle still
 * out counts up to now.
 */
@Service
class DefaultReportingService implements ReportingService {

    private final BookingRepository bookings;
    private final CycleRepository cycles;
    private final Clock clock;

    DefaultReportingService(BookingRepository bookings, CycleRepository cycles, Clock clock) {
        this.bookings = bookings;
        this.cycles = cycles;
        this.clock = clock;
    }

    @Override
    public UsageReport getWeeklyUsageReport() {
        Instant now = clock.instant();
        return getUsageReport(now.minus(Duration.ofDays(7)), now);
    }

    @Override
    @Transactional(readOnly = true)
    public UsageReport getUsageReport(Instant from, Instant to) {
        Instant now = clock.instant();
        List<Booking> rentals = bookings.findRentalsOverlapping(from, to);

        List<CycleUsage> rows = new ArrayList<>();
        for (Cycle cycle : cycles.findAllByOrderByLabelAsc()) {
            int count = 0;
            Duration used = Duration.ZERO;
            for (Booking b : rentals) {
                if (!b.getCycle().getId().equals(cycle.getId())) {
                    continue;
                }
                count++;
                used = used.plus(overlap(b, from, to, now));
            }
            rows.add(new CycleUsage(cycle.getLabel(), count, used.toMinutes() / 60.0));
        }
        return new UsageReport(from, to, rows);
    }

    /** The part of [pickup, return-or-now) that falls inside [from, to). */
    private static Duration overlap(Booking b, Instant from, Instant to, Instant now) {
        Instant end = b.getReturnedAt() != null ? b.getReturnedAt() : now;
        Instant start = b.getPickedUpAt().isAfter(from) ? b.getPickedUpAt() : from;
        Instant stop = end.isBefore(to) ? end : to;
        return stop.isAfter(start) ? Duration.between(start, stop) : Duration.ZERO;
    }
}
