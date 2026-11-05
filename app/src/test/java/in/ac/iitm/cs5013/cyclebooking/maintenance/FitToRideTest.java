package in.ac.iitm.cs5013.cyclebooking.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CyclePart;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.FlagSource;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.persistence.PartCondition;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** ADR 0005's rule: only a confirmed broken critical part keeps a cycle off the road. */
class FitToRideTest {

    private static final Cycle CYCLE = new Cycle("FIT-01");

    private static CyclePart part(Part part, PartCondition condition) {
        return new CyclePart(CYCLE, part, condition, FlagSource.GUARD, Instant.parse("2026-11-05T05:00:00Z"), null);
    }

    @Test
    void cycleWithNoRecordedPartsIsAvailable() {
        assertThat(FitToRide.nextState(List.of())).isEqualTo(CycleState.AVAILABLE);
    }

    @Test
    void brokenBrakesSendTheCycleToRepair() {
        assertThat(FitToRide.nextState(List.of(part(Part.BRAKES, PartCondition.BROKEN))))
                .isEqualTo(CycleState.UNDER_REPAIR);
    }

    @Test
    void brokenBellOnlyWarnsTheCycleStaysAvailable() {
        assertThat(FitToRide.nextState(List.of(part(Part.BELL, PartCondition.BROKEN), part(Part.LOCK, PartCondition.BROKEN))))
                .isEqualTo(CycleState.AVAILABLE);
    }

    @Test
    void reportedButUnconfirmedCriticalPartDoesNotBlockTheCycle() {
        assertThat(FitToRide.nextState(List.of(part(Part.BRAKES, PartCondition.REPORTED))))
                .isEqualTo(CycleState.AVAILABLE);
    }

    @Test
    void repairedCriticalPartNoLongerBlocks() {
        assertThat(FitToRide.nextState(List.of(part(Part.TYRE, PartCondition.OK), part(Part.BELL, PartCondition.BROKEN))))
                .isEqualTo(CycleState.AVAILABLE);
    }
}
