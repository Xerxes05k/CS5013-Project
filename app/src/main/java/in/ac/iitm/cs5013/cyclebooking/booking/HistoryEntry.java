package in.ac.iitm.cs5013.cyclebooking.booking;

import java.time.Instant;

/**
 * One of the resident's rentals, as the History tab shows it.
 *
 * @param hours how long the cycle was out: pickup to return, or to now if still out; 0 if never picked up
 * @param late  true if it was held longer than the 24-hour limit
 */
public record HistoryEntry(String cycleLabel, Instant bookedAt, Instant pickedUpAt, Instant returnedAt,
        double hours, boolean late) {

    /** Booked (not picked up yet), Out, or Returned. */
    public String status() {
        if (pickedUpAt == null) {
            return "Booked";
        }
        return returnedAt == null ? "Out" : "Returned";
    }
}
