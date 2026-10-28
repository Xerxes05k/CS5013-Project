package in.ac.iitm.cs5013.cyclebooking.maintenance;

import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlag;
import java.util.List;

/**
 * Maintenance Comment Module: what residents and the guard say about a cycle's
 * condition. The raw text is always kept and shown; flags only help the GS sort it
 * (ADR 0003).
 */
public interface MaintenanceService {

    /** Longest resident comment accepted. Matches the {@code comments.text} column. */
    int MAX_COMMENT_LENGTH = 2000;

    /** Longest guard note accepted at return. */
    int MAX_GUARD_NOTE_LENGTH = 500;

    /**
     * Label of the flag raised by a guard's free-text note that names no part. Pages show
     * it as "Guard note"; every other label is a part name.
     */
    String GUARD_NOTE_LABEL = "guard_note";

    /**
     * Stores the resident's comment on a returned booking, one per booking. Returns the
     * flags it raised: none in v1 (week 4). The week-5 classifier fills this in.
     * Throws {@link CommentRejectedException} for a blank or too-long comment, a booking
     * not yet returned, or a second comment.
     */
    List<MaintenanceFlag> submitComment(long bookingId, String text);

    /**
     * Records the guard's note on the cycle's condition, made at return, as a GUARD flag
     * on the cycle. A blank note records nothing. Called by the Pickup/Return Module
     * inside the return, so the note and the return are saved together or not at all.
     */
    void recordGuardNote(long bookingId, String note);
}
