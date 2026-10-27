package in.ac.iitm.cs5013.cyclebooking.reporting;

import in.ac.iitm.cs5013.cyclebooking.persistence.FlagSource;
import java.time.Instant;

/**
 * One open maintenance flag as the GS's list shows it. The raw words always travel with
 * the label, because a flag only sorts the list; a person reads the words (ADR 0003).
 *
 * @param words the guard's note for a GUARD flag, the resident's comment for a COMMENT flag
 */
public record MaintenanceItem(long flagId, String cycleLabel, String label, FlagSource source, String words,
        Instant raisedAt) {
}
