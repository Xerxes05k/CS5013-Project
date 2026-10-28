package in.ac.iitm.cs5013.cyclebooking.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.persistence.PartCondition;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PartStatusTest {

    @Test
    void criticalComesFromThePart() {
        Instant t = Instant.parse("2026-10-28T05:00:00Z");

        assertThat(new PartStatus(Part.BRAKES, PartCondition.OK, t, null).critical()).isTrue();
        assertThat(new PartStatus(Part.BELL, PartCondition.BROKEN, t, "missing").critical()).isFalse();
    }
}
