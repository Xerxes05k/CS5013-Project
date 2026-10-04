package in.ac.iitm.cs5013.cyclebooking.booking;

import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.IdCheck;
import java.time.Instant;

/** A resident's open booking, as the "My cycle" page shows it. */
public record CurrentRental(long bookingId, String cycleLabel, CycleState cycleState, Instant createdAt,
        Instant pickedUpAt, IdCheck idCheck, boolean overdue) {

    public boolean awaitingPickup() {
        return pickedUpAt == null;
    }

    public boolean guardShouldCheckCard() {
        return idCheck == IdCheck.TYPED_ROLL;
    }
}
