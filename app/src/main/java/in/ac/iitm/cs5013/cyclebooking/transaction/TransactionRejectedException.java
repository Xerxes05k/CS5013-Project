package in.ac.iitm.cs5013.cyclebooking.transaction;

/** A pickup or return that was refused, with a reason the resident should see. */
public class TransactionRejectedException extends RuntimeException {

    public TransactionRejectedException(String message) {
        super(message);
    }
}
