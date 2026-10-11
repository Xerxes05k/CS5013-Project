package in.ac.iitm.cs5013.cyclebooking.transaction;

import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;

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
     * over and the resident types what the guard says into {@code guardConditionNote};
     * blank means nothing wrong. A non-blank note is passed to the Maintenance Module.
     */
    CycleState confirmReturn(long bookingId, String passcode, String guardConditionNote);
}
