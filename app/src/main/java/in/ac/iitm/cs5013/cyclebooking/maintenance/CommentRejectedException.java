package in.ac.iitm.cs5013.cyclebooking.maintenance;

/** A comment that was not stored, with a reason the resident should see. */
public class CommentRejectedException extends RuntimeException {

    public CommentRejectedException(String message) {
        super(message);
    }
}
