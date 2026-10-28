package in.ac.iitm.cs5013.cyclebooking.maintenance;

import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.persistence.PartCondition;
import java.time.Instant;

/**
 * One part of one cycle as pages show it: the cycle detail parts grid and the GS
 * screens (ADR 0005).
 *
 * @param note the last guard's or GS's words about this part, or null
 */
public record PartStatus(Part part, PartCondition condition, Instant updatedAt, String note) {

    /** Read from the part, so it can't disagree with the fit-to-ride rule. */
    public boolean critical() {
        return part.critical();
    }
}
