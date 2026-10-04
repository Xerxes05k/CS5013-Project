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
     * ISSUED -> AVAILABLE. Returns the cycle's new state. The guard's condition note
     * joins this call in week 4, with the Maintenance Module.
     */
    CycleState confirmReturn(long bookingId, String passcode);
}
