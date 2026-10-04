package in.ac.iitm.cs5013.cyclebooking.rules;

import static org.assertj.core.api.Assertions.assertThat;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.rules.EligibilityResult.Reason;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Rule Engine test, against real SQLite so the rolling-week count query is exercised
 * too. A fixed clock puts every case exactly on its boundary.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DefaultRuleServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private TestEntityManager entityManager;

    private RuleService rules;
    private Resident resident;
    private Cycle cycle;

    @BeforeEach
    void setUp() {
        rules = new DefaultRuleService(bookings, Clock.fixed(NOW, ZoneOffset.UTC));
        resident = entityManager.persist(
                new Resident("CE24B050", "Rule Test", "Jamuna", "ce24b050@smail.iitm.ac.in", "hash"));
        cycle = entityManager.persist(new Cycle("RULE-01"));
    }

    /** A finished rental (picked up and returned) created the given time before NOW. */
    private void returnedBooking(Duration ago) {
        Booking b = new Booking(resident, cycle, NOW.minus(ago));
        b.setPickedUpAt(NOW.minus(ago));
        b.setReturnedAt(NOW.minus(ago).plus(Duration.ofHours(2)));
        entityManager.persist(b);
    }

    private void heldFor(Duration held) {
        Booking b = new Booking(resident, cycle, NOW.minus(held));
        b.setPickedUpAt(NOW.minus(held));
        entityManager.persist(b);
    }

    @Test
    void checkEligibility_allowsResidentWithNoBookings() {
        assertThat(rules.checkEligibility(resident.getId())).isEqualTo(new EligibilityResult(true, Reason.OK));
    }

    @Test
    void checkEligibility_allowsThirdBookingButDeniesFourthInRollingWeek() {
        returnedBooking(Duration.ofDays(1));
        returnedBooking(Duration.ofDays(3));
        assertThat(rules.checkEligibility(resident.getId()).allowed()).isTrue();

        returnedBooking(Duration.ofDays(5));
        EligibilityResult fourth = rules.checkEligibility(resident.getId());
        assertThat(fourth.allowed()).isFalse();
        assertThat(fourth.reason()).isEqualTo(Reason.WEEKLY_LIMIT);
    }

    @Test
    void checkEligibility_forgetsBookingsOlderThanSevenDays() {
        returnedBooking(Duration.ofDays(1));
        returnedBooking(Duration.ofDays(3));
        returnedBooking(Duration.ofDays(7).plusMinutes(1));

        assertThat(rules.checkEligibility(resident.getId()).allowed()).isTrue();
    }

    @Test
    void checkEligibility_cycleHeld23HoursIsStillInUseNotOverdue() {
        heldFor(Duration.ofHours(23));

        EligibilityResult result = rules.checkEligibility(resident.getId());
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo(Reason.ALREADY_HOLDING);
    }

    @Test
    void checkEligibility_cycleHeld25HoursIsOverdue() {
        heldFor(Duration.ofHours(25));

        EligibilityResult result = rules.checkEligibility(resident.getId());
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo(Reason.OVERDUE_RETURN);
    }
}
