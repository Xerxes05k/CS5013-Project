package in.ac.iitm.cs5013.cyclebooking.booking;

import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.idverify.ExtractedIdentity;
import java.util.List;
import java.util.Optional;

/** Booking Module: availability and reservation over the cycle pool. */
public interface BookingService {

    List<CycleStatus> getAvailability();

    /**
     * Books the cycle for the resident once the scanned ID card matches their login.
     * Throws {@link IdCheckFailedException} if the card does not match, or
     * {@link BookingRejectedException} for any other reason the resident should see.
     */
    BookingConfirmation createBooking(ResidentIdentity resident, long cycleId, ExtractedIdentity scannedCard);

    /**
     * The fallback when the card cannot be read: the resident types their roll number.
     * The booking is marked so the guard checks the physical card at pickup. The web
     * layer only offers this after {@code IdVerificationService.MAX_FAILED_SCANS}
     * failed scans.
     */
    BookingConfirmation createBookingWithTypedRoll(ResidentIdentity resident, long cycleId, String typedRoll);

    /** The resident's booking that has not been returned yet, if any. */
    Optional<CurrentRental> currentRental(long residentId);

    /** All of the resident's rentals, newest first, for the History tab. */
    List<HistoryEntry> history(long residentId);

    /**
     * Cancels the resident's booking before pickup and frees the cycle. Returns the
     * cycle's label. Throws {@link BookingRejectedException} if there is no open booking
     * or the cycle has already been picked up (that is a return, done at the stand).
     */
    String cancelBooking(long residentId);
}
