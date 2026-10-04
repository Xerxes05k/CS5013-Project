package in.ac.iitm.cs5013.cyclebooking.booking;

/**
 * The booking was refused because the ID card did not prove the resident's identity.
 * Kept separate from other refusals so the web layer can count failed scans.
 */
public class IdCheckFailedException extends BookingRejectedException {

    public IdCheckFailedException(String message) {
        super(message);
    }
}
