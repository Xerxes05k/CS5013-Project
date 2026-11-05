package in.ac.iitm.cs5013.cyclebooking.maintenance;

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
import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlag;
import in.ac.iitm.cs5013.cyclebooking.persistence.MaintenanceFlagRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.persistence.PartCondition;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private final CyclePartRepository parts;
    private final CycleRepository cycles;
    private final Clock clock;

    DefaultMaintenanceService(BookingRepository bookings, CommentRepository comments,
            MaintenanceFlagRepository flags, CyclePartRepository parts, CycleRepository cycles, Clock clock) {
        this.bookings = bookings;
        this.comments = comments;
        this.flags = flags;
        this.parts = parts;
        this.cycles = cycles;
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
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No booking " + bookingId));
        Cycle cycle = booking.getCycle();
        Instant now = clock.instant();
        String words = trimmed.isEmpty() ? null : trimmed;

        if (brokenParts.isEmpty() && words != null) {
            flags.save(new MaintenanceFlag(cycle, null, GUARD_NOTE_LABEL, FlagSource.GUARD, now, words));
        }
        // Declaration order, so the flags are saved in the same order every time.
        brokenParts.stream().sorted().forEach(part -> {
            flags.save(new MaintenanceFlag(cycle, null, part.label(), FlagSource.GUARD, now, words));
            mark(cycle, part, PartCondition.BROKEN, FlagSource.GUARD, now, words);
        });
        // Over every recorded part, not just today's: a part broken earlier still counts.
        return FitToRide.nextState(parts.findByCycleId(cycle.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartStatus> partsOf(long cycleId) {
        Map<Part, CyclePart> recorded = new EnumMap<>(Part.class);
        parts.findByCycleId(cycleId).forEach(p -> recorded.put(p.getPart(), p));
        return Arrays.stream(Part.values())
                .map(part -> {
                    CyclePart p = recorded.get(part);
                    // No row: this part has never changed, so it is OK.
                    return p == null ? new PartStatus(part, PartCondition.OK, null, null)
                            : new PartStatus(part, p.getCondition(), p.getUpdatedAt(), p.getNote());
                })
                .toList();
    }

    @Override
    @Transactional
    public CycleState recordRepair(long cycleId, Part part, String note) {
        String trimmed = note == null ? "" : note.strip();
        if (trimmed.length() > MAX_GUARD_NOTE_LENGTH) {
            throw new IllegalArgumentException("Note longer than " + MAX_GUARD_NOTE_LENGTH + " characters");
        }
        Cycle cycle = cycles.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("No cycle " + cycleId));
        Instant now = clock.instant();
        String words = trimmed.isEmpty() ? null : trimmed;

        mark(cycle, part, PartCondition.OK, FlagSource.GS, now, words);
        // The issue is fixed, so it leaves the GS's open list...
        flags.findByCycleIdAndLabelAndResolvedAtIsNull(cycleId, part.label()).forEach(f -> f.setResolvedAt(now));
        // ...and the repair itself is kept as history, already resolved.
        MaintenanceFlag repair = new MaintenanceFlag(cycle, null, part.label(), FlagSource.GS, now, words);
        repair.setResolvedAt(now);
        flags.save(repair);

        // Only a cycle back at the stand moves; a repair never takes one from a rental.
        if (cycle.getState() == CycleState.AVAILABLE || cycle.getState() == CycleState.UNDER_REPAIR) {
            cycle.setState(FitToRide.nextState(parts.findByCycleId(cycleId)));
        }
        return cycle.getState();
    }

    /** Sets a part's current condition, adding its row the first time it changes. */
    private void mark(Cycle cycle, Part part, PartCondition condition, FlagSource by, Instant at, String note) {
        parts.findByCycleIdAndPart(cycle.getId(), part).ifPresentOrElse(
                p -> p.change(condition, by, at, note),
                () -> parts.save(new CyclePart(cycle, part, condition, by, at, note)));
    }
}
