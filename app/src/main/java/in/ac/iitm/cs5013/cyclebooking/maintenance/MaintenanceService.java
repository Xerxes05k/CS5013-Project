package in.ac.iitm.cs5013.cyclebooking.maintenance;

import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlag;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import java.util.List;
import java.util.Set;

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
     * Records what the guard found when the cycle came back (ADR 0005). Each part in
     * {@code brokenParts} becomes a GUARD flag labelled with that part and carrying the
     * guard's note. A note with no part ticked becomes one {@link #GUARD_NOTE_LABEL}
     * flag; no parts and a blank note record nothing.
     *
     * Returns the state the cycle should take after the return. Until each cycle's part
     * conditions are stored, that is always AVAILABLE; the fit-to-ride rule replaces it
     * in the cycle-parts PR. Called by the Pickup/Return Module inside the return, so the
     * flags and the return are saved together or not at all.
     *
     * @param brokenParts never null; empty when the guard found nothing broken
     */
    CycleState recordInspection(long bookingId, Set<Part> brokenParts, String note);
}
