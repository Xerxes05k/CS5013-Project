package in.ac.iitm.cs5013.cyclebooking.transaction;

import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import java.util.Set;

/**
 * Pickup/Return Module: the guard-witnessed step at the cycle stand. The resident
 * types their passcode on their own phone while the guard watches, then the guard
 * hands over or takes back the cycle. The guard has no device.
 *
 * A wrong passcode, or a step out of order, throws {@link TransactionRejectedException}
 * and changes nothing.
 */
public interface TransactionService {

    /** BOOKED -> ISSUED. Returns the cycle's new state. */
    CycleState confirmPickup(long bookingId, String passcode);

    /**
     * ISSUED -> AVAILABLE. Returns the cycle's new state. The guard looks the cycle
     * over; the resident ticks the parts the guard found broken and types anything else
     * the guard says into {@code guardConditionNote}. Nothing ticked and a blank note
     * mean nothing wrong. Both go to the Maintenance Module (ADR 0005).
     *
     * @param brokenParts never null; empty when the guard found nothing broken
     */
    CycleState confirmReturn(long bookingId, String passcode, Set<Part> brokenParts, String guardConditionNote);
}
