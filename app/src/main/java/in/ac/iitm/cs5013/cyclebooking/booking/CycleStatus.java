package in.ac.iitm.cs5013.cyclebooking.booking;

import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import java.util.List;

/**
 * What the availability page needs to know about one cycle.
 *
 * @param knownIssues the parts that are not OK (reported or broken), in {@link Part}
 *                    order, so residents see "bell broken" before they book (ADR 0005)
 */
public record CycleStatus(long id, String label, CycleState state, List<Part> knownIssues) {

    public boolean bookable() {
        return state == CycleState.AVAILABLE;
    }
}
