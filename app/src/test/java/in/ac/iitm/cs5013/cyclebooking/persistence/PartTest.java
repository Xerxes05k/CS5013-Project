package in.ac.iitm.cs5013.cyclebooking.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** The critical / non-critical split that decides UNDER_REPAIR (ADR 0005, decision 2). */
class PartTest {

    @Test
    void sixRidingPartsAreCriticalAndLockAndBellAreNot() {
        assertThat(Arrays.stream(Part.values()).filter(Part::critical))
                .containsExactlyInAnyOrder(Part.TYRE, Part.BRAKES, Part.CHAIN, Part.SEAT, Part.PEDAL, Part.HANDLEBAR);
        assertThat(Part.LOCK.critical()).isFalse();
        assertThat(Part.BELL.critical()).isFalse();
    }

    @Test
    void labelIsTheLowercaseNameTheClassifierUses() {
        assertThat(Part.BRAKES.label()).isEqualTo("brakes");
        assertThat(Part.HANDLEBAR.label()).isEqualTo("handlebar");
    }
}
