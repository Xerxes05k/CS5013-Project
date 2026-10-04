package in.ac.iitm.cs5013.cyclebooking.reporting;

import java.time.Instant;
import java.util.List;

/** Hours each cycle spent with a resident over a time window, every cycle listed. */
public record UsageReport(Instant from, Instant to, List<CycleUsage> cycles) {

    /** One cycle's row. {@code rentals} counts every rental that touched the window. */
    public record CycleUsage(String cycleLabel, int rentals, double hours) {
    }

    public double totalHours() {
        return cycles.stream().mapToDouble(CycleUsage::hours).sum();
    }
}
