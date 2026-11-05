package in.ac.iitm.cs5013.cyclebooking.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Comment;
import in.ac.iitm.cs5013.cyclebooking.persistence.CommentRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CyclePart;
import in.ac.iitm.cs5013.cyclebooking.persistence.CyclePartRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.FlagSource;
import in.ac.iitm.cs5013.cyclebooking.persistence.IdCheck;
import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlag;
import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlagRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.persistence.PartCondition;
import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Maintenance v1 (week 4): the comment is stored exactly as the resident wrote it
 * (trimmed), once per returned booking, and the guard's findings become GUARD flags.
 */
@SpringBootTest
class DefaultMaintenanceServiceTest {

    @Autowired
    private MaintenanceService maintenance;

    @Autowired
    private ResidentRepository residents;

    @Autowired
    private CycleRepository cycles;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private CommentRepository comments;

    @Autowired
    private MaintenanceFlagRepository flags;

    @Autowired
    private CyclePartRepository parts;

    private long returnedId;
    private long openId;
    private long cycleId;

    @BeforeEach
    void seed() {
        Resident r = residents.save(new Resident("CE24B071", "Maint Test", "Jamuna",
                "ce24b071@smail.iitm.ac.in", "unused-hash"));
        Cycle c = cycles.save(new Cycle("MNT-01"));
        cycleId = c.getId();
        Instant start = Instant.now().minus(Duration.ofHours(3));

        Booking returned = new Booking(r, c, start, IdCheck.CARD_SCAN);
        returned.setPickedUpAt(start);
        returned.setReturnedAt(start.plus(Duration.ofHours(1)));
        returnedId = bookings.save(returned).getId();

        Booking open = new Booking(r, c, start.plus(Duration.ofHours(2)), IdCheck.CARD_SCAN);
        open.setPickedUpAt(start.plus(Duration.ofHours(2)));
        openId = bookings.save(open).getId();
    }

    @AfterEach
    void cleanUp() {
        parts.deleteAll();
        flags.deleteAll();
        comments.deleteAll();
        bookings.deleteAll();
        cycles.deleteAll();
        residents.deleteAll();
    }

    @Test
    void submitComment_storesRawTextAndRaisesNoFlagsYet() {
        var raised = maintenance.submitComment(returnedId, "  chain keeps slipping off\n");

        assertThat(raised).isEmpty();
        assertThat(comments.findByBookingId(returnedId)).singleElement()
                .extracting(Comment::getText).isEqualTo("chain keeps slipping off");
    }

    @Test
    void submitComment_beforeReturnIsRefused() {
        assertThatThrownBy(() -> maintenance.submitComment(openId, "seat is loose"))
                .isInstanceOf(CommentRejectedException.class)
                .hasMessageContaining("returned");
        assertThat(comments.findByBookingId(openId)).isEmpty();
    }

    @Test
    void submitComment_secondCommentOnSameRentalIsRefused() {
        maintenance.submitComment(returnedId, "bell missing");

        assertThatThrownBy(() -> maintenance.submitComment(returnedId, "also the seat"))
                .isInstanceOf(CommentRejectedException.class)
                .hasMessageContaining("already");
        assertThat(comments.findByBookingId(returnedId)).hasSize(1);
    }

    @Test
    void submitComment_blankOrTooLongIsRefused() {
        assertThatThrownBy(() -> maintenance.submitComment(returnedId, "   "))
                .isInstanceOf(CommentRejectedException.class);
        assertThatThrownBy(() -> maintenance.submitComment(returnedId,
                "x".repeat(MaintenanceService.MAX_COMMENT_LENGTH + 1)))
                .isInstanceOf(CommentRejectedException.class);
        assertThat(comments.findByBookingId(returnedId)).isEmpty();
    }

    @Test
    void recordInspection_noteWithNoPartTicked_raisesOneGuardNoteFlag() {
        maintenance.recordInspection(returnedId, Set.of(), "rear tyre soft");

        assertThat(flags.findByCycleId(cycleId)).singleElement().satisfies(flag -> {
            assertThat(flag.getSource()).isEqualTo(FlagSource.GUARD);
            assertThat(flag.getLabel()).isEqualTo(MaintenanceService.GUARD_NOTE_LABEL);
            assertThat(flag.getNote()).isEqualTo("rear tyre soft");
            assertThat(flag.getResolvedAt()).isNull();
        });
    }

    @Test
    void recordInspection_raisesOneGuardFlagPerBrokenPartWithTheGuardsWords() {
        maintenance.recordInspection(returnedId, Set.of(Part.BELL, Part.BRAKES), "front brake loose");

        assertThat(flags.findByCycleId(cycleId))
                .extracting(MaintenanceFlag::getLabel, MaintenanceFlag::getSource, MaintenanceFlag::getNote)
                .containsExactlyInAnyOrder(
                        tuple("brakes", FlagSource.GUARD, "front brake loose"),
                        tuple("bell", FlagSource.GUARD, "front brake loose"));
    }

    @Test
    void recordInspection_nothingFound_recordsNothingAndLeavesTheCycleAvailable() {
        assertThat(maintenance.recordInspection(returnedId, Set.of(), "")).isEqualTo(CycleState.AVAILABLE);
        assertThat(maintenance.recordInspection(returnedId, Set.of(), null)).isEqualTo(CycleState.AVAILABLE);

        assertThat(flags.findByCycleId(cycleId)).isEmpty();
    }

    @Test
    void partsOf_listsAllEightPartsWithUnrecordedOnesOk() {
        Cycle cycle = cycles.findById(cycleId).orElseThrow();
        parts.save(new CyclePart(cycle, Part.BRAKES, PartCondition.BROKEN, FlagSource.GUARD,
                Instant.parse("2026-11-05T05:00:00Z"), "front brake loose"));

        List<PartStatus> statuses = maintenance.partsOf(cycleId);

        assertThat(statuses).extracting(PartStatus::part).containsExactly(Part.values());
        assertThat(statuses.get(Part.BRAKES.ordinal())).satisfies(brakes -> {
            assertThat(brakes.condition()).isEqualTo(PartCondition.BROKEN);
            assertThat(brakes.note()).isEqualTo("front brake loose");
            assertThat(brakes.critical()).isTrue();
        });
        assertThat(statuses.get(Part.BELL.ordinal())).satisfies(bell -> {
            assertThat(bell.condition()).isEqualTo(PartCondition.OK);
            assertThat(bell.updatedAt()).isNull();
        });
    }

    @Test
    void recordInspection_brokenCriticalPart_isMarkedBrokenAndSendsTheCycleToRepair() {
        CycleState next = maintenance.recordInspection(returnedId, Set.of(Part.BRAKES), "front brake loose");

        assertThat(next).isEqualTo(CycleState.UNDER_REPAIR);
        assertThat(parts.findByCycleIdAndPart(cycleId, Part.BRAKES)).hasValueSatisfying(brakes -> {
            assertThat(brakes.getCondition()).isEqualTo(PartCondition.BROKEN);
            assertThat(brakes.getChangedBy()).isEqualTo(FlagSource.GUARD);
            assertThat(brakes.getNote()).isEqualTo("front brake loose");
        });
    }

    @Test
    void recordInspection_brokenBellOnly_isMarkedBrokenButTheCycleStaysAvailable() {
        assertThat(maintenance.recordInspection(returnedId, Set.of(Part.BELL), "")).isEqualTo(CycleState.AVAILABLE);
        assertThat(parts.findByCycleIdAndPart(cycleId, Part.BELL)).get()
                .extracting(CyclePart::getCondition).isEqualTo(PartCondition.BROKEN);
    }

    @Test
    void recordInspection_partAlreadyRecordedIsUpdatedNotDuplicated() {
        Cycle cycle = cycles.findById(cycleId).orElseThrow();
        parts.save(new CyclePart(cycle, Part.TYRE, PartCondition.REPORTED, FlagSource.COMMENT,
                Instant.parse("2026-11-04T05:00:00Z"), null));

        maintenance.recordInspection(returnedId, Set.of(Part.TYRE), "rear tyre flat");

        assertThat(parts.findByCycleId(cycleId)).singleElement().satisfies(tyre -> {
            assertThat(tyre.getCondition()).isEqualTo(PartCondition.BROKEN);
            assertThat(tyre.getChangedBy()).isEqualTo(FlagSource.GUARD);
        });
    }

    @Test
    void recordInspection_criticalPartBrokenEarlierStillCountsWhenNothingNewIsFound() {
        Cycle cycle = cycles.findById(cycleId).orElseThrow();
        parts.save(new CyclePart(cycle, Part.CHAIN, PartCondition.BROKEN, FlagSource.GS,
                Instant.parse("2026-11-04T05:00:00Z"), null));

        assertThat(maintenance.recordInspection(returnedId, Set.of(), "")).isEqualTo(CycleState.UNDER_REPAIR);
    }
}
