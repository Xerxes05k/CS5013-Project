package in.ac.iitm.cs5013.cyclebooking.booking;

import java.time.Instant;

/** Returned to the caller instead of the JPA entity, so callers stay out of persistence. */
public record BookingConfirmation(long bookingId, String cycleLabel, Instant createdAt) {
}
