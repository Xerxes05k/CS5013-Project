package in.ac.iitm.cs5013.cyclebooking.maintenance;

import in.ac.iitm.cs5013.cyclebooking.persistence.CyclePart;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.PartCondition;
import java.util.Collection;

/**
 * The one rule that decides whether a cycle back at the stand can go out again
 * (ADR 0005, decision 3). Kept in one place so the return and a GS repair can't
 * disagree about it.
 */
final class FitToRide {

    private FitToRide() {
    }

    /**
     * UNDER_REPAIR if any critical part is BROKEN, otherwise AVAILABLE. A REPORTED part
     * doesn't count: nobody has confirmed it yet. A part with no row is OK.
     */
    static CycleState nextState(Collection<CyclePart> recordedParts) {
        boolean brokenCritical = recordedParts.stream()
                .anyMatch(p -> p.getPart().critical() && p.getCondition() == PartCondition.BROKEN);
        return brokenCritical ? CycleState.UNDER_REPAIR : CycleState.AVAILABLE;
    }
}
