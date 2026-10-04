package in.ac.iitm.cs5013.cyclebooking.booking;

/** A booking that was refused for a reason the resident should see. */
public class BookingRejectedException extends RuntimeException {

    public BookingRejectedException(String message) {
        super(message);
    }
}
