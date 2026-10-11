package in.ac.iitm.cs5013.cyclebooking.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Comment;
import in.ac.iitm.cs5013.cyclebooking.persistence.CommentRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.FlagSource;
import in.ac.iitm.cs5013.cyclebooking.persistence.IdCheck;
import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlagRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Maintenance v1 (week 4): the comment is stored exactly as the resident wrote it
 * (trimmed), once per returned booking, and a guard's note becomes a GUARD flag.
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
    void recordGuardNote_raisesGuardFlagWithTheGuardsWords() {
        maintenance.recordGuardNote(returnedId, "rear tyre soft");

        assertThat(flags.findByCycleId(cycleId)).singleElement().satisfies(flag -> {
            assertThat(flag.getSource()).isEqualTo(FlagSource.GUARD);
            assertThat(flag.getLabel()).isEqualTo(DefaultMaintenanceService.GUARD_NOTE_LABEL);
            assertThat(flag.getNote()).isEqualTo("rear tyre soft");
            assertThat(flag.getResolvedAt()).isNull();
        });
    }

    @Test
    void recordGuardNote_blankRecordsNothing() {
        maintenance.recordGuardNote(returnedId, "");
        maintenance.recordGuardNote(returnedId, null);

        assertThat(flags.findByCycleId(cycleId)).isEmpty();
    }
}
