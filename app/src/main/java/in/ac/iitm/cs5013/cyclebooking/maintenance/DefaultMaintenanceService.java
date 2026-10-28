package in.ac.iitm.cs5013.cyclebooking.maintenance;

import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Comment;
import in.ac.iitm.cs5013.cyclebooking.persistence.CommentRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.FlagSource;
import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlag;
import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlagRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v1 (week 4): comments and guard notes are stored, nothing is classified yet. The
 * guard's note becomes a flag straight away, because a guard saying something is
 * wrong is worth the GS's attention whatever the words are.
 */
@Service
class DefaultMaintenanceService implements MaintenanceService {

    private final BookingRepository bookings;
    private final CommentRepository comments;
    private final MaintenanceFlagRepository flags;
    private final Clock clock;

    DefaultMaintenanceService(BookingRepository bookings, CommentRepository comments,
            MaintenanceFlagRepository flags, Clock clock) {
        this.bookings = bookings;
        this.comments = comments;
        this.flags = flags;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<MaintenanceFlag> submitComment(long bookingId, String text) {
        String trimmed = text == null ? "" : text.strip();
        if (trimmed.isEmpty()) {
            throw new CommentRejectedException("The comment is empty. Write something, or skip.");
        }
        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            throw new CommentRejectedException("Please keep the comment under " + MAX_COMMENT_LENGTH + " characters.");
        }
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new CommentRejectedException("No such booking."));
        if (booking.getReturnedAt() == null) {
            throw new CommentRejectedException("You can comment once the cycle has been returned.");
        }
        if (!comments.findByBookingId(bookingId).isEmpty()) {
            throw new CommentRejectedException("You have already commented on this rental.");
        }

        comments.save(new Comment(booking, trimmed, clock.instant()));
        // The week-5 classifier turns the comment into flags here.
        return List.of();
    }

    @Override
    @Transactional
    public CycleState recordInspection(long bookingId, Set<Part> brokenParts, String note) {
        String trimmed = note == null ? "" : note.strip();
        if (trimmed.length() > MAX_GUARD_NOTE_LENGTH) {
            throw new IllegalArgumentException("Guard note longer than " + MAX_GUARD_NOTE_LENGTH + " characters");
        }
        if (brokenParts.isEmpty() && trimmed.isEmpty()) {
            return CycleState.AVAILABLE;
        }
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No booking " + bookingId));
        Instant now = clock.instant();
        String words = trimmed.isEmpty() ? null : trimmed;

        if (brokenParts.isEmpty()) {
            flags.save(new MaintenanceFlag(booking.getCycle(), null, GUARD_NOTE_LABEL, FlagSource.GUARD, now, words));
        }
        // Declaration order, so the flags are saved in the same order every time.
        brokenParts.stream().sorted().forEach(part -> flags.save(
                new MaintenanceFlag(booking.getCycle(), null, part.label(), FlagSource.GUARD, now, words)));
        return CycleState.AVAILABLE;
    }
}
