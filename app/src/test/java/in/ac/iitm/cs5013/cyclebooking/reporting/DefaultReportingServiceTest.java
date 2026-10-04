package in.ac.iitm.cs5013.cyclebooking.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.reporting.UsageReport.CycleUsage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Reporting test from the architecture's test plan: seeded bookings give exactly the
 * expected usage hours per cycle. The report window is 7 days ending at NOW.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DefaultReportingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final Instant WEEK_START = Instant.parse("2026-10-02T12:00:00Z");

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private CycleRepository cycles;

    @Autowired
    private TestEntityManager entityManager;

    private ReportingService reporting;
    private Resident resident;

    @BeforeEach
    void setUp() {
        reporting = new DefaultReportingService(bookings, cycles, Clock.fixed(NOW, ZoneOffset.UTC));
        resident = entityManager.persist(
                new Resident("CE24B080", "Report Test", "Jamuna", "ce24b080@smail.iitm.ac.in", "hash"));
    }

    private Cycle cycle(String label) {
        return entityManager.persist(new Cycle(label));
    }

    private void rental(Cycle c, String pickedUp, String returned) {
        Booking b = new Booking(resident, c, Instant.parse(pickedUp));
        b.setPickedUpAt(Instant.parse(pickedUp));
        b.setReturnedAt(returned == null ? null : Instant.parse(returned));
        entityManager.persist(b);
    }

    @Test
    void getWeeklyUsageReport_givesExactHoursPerCycle() {
        Cycle a = cycle("REP-A");
        Cycle b = cycle("REP-B");
        Cycle c = cycle("REP-C");
        Cycle idle = cycle("REP-D");

        // REP-A: two rentals fully inside the week, 3 h + 1.5 h.
        rental(a, "2026-10-05T08:00:00Z", "2026-10-05T11:00:00Z");
        rental(a, "2026-10-07T16:00:00Z", "2026-10-07T17:30:00Z");
        // REP-B: picked up 2 h before the week started, returned 4 h into it -> 4 h counted.
        rental(b, "2026-10-02T10:00:00Z", "2026-10-02T16:00:00Z");
        // REP-B: entirely before the week -> not counted at all.
        rental(b, "2026-09-30T10:00:00Z", "2026-09-30T12:00:00Z");
        // REP-C: still out, picked up 5 h before NOW -> 5 h so far.
        rental(c, "2026-10-09T07:00:00Z", null);
        // Booked but never picked up: not usage.
        entityManager.persist(new Booking(resident, idle, Instant.parse("2026-10-08T09:00:00Z")));

        UsageReport report = reporting.getWeeklyUsageReport();

        assertThat(report.from()).isEqualTo(WEEK_START);
        assertThat(report.to()).isEqualTo(NOW);
        assertThat(report.cycles()).containsExactly(
                new CycleUsage("REP-A", 2, 4.5),
                new CycleUsage("REP-B", 1, 4.0),
                new CycleUsage("REP-C", 1, 5.0),
                new CycleUsage("REP-D", 0, 0.0));
        assertThat(report.totalHours()).isEqualTo(13.5);
    }
}
