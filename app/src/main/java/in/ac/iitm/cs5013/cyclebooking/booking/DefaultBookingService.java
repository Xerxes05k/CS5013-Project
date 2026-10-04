package in.ac.iitm.cs5013.cyclebooking.booking;

import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.idverify.ExtractedIdentity;
import in.ac.iitm.cs5013.cyclebooking.idverify.IdVerificationService;
import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.BookingRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.Cycle;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.IdCheck;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import in.ac.iitm.cs5013.cyclebooking.rules.EligibilityResult;
import in.ac.iitm.cs5013.cyclebooking.rules.RuleService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultBookingService implements BookingService {

    private final CycleRepository cycles;
    private final ResidentRepository residents;
    private final BookingRepository bookings;
    private final RuleService rules;
    private final IdVerificationService idVerification;
    private final Clock clock;

    DefaultBookingService(CycleRepository cycles, ResidentRepository residents, BookingRepository bookings,
            RuleService rules, IdVerificationService idVerification, Clock clock) {
        this.cycles = cycles;
        this.residents = residents;
        this.bookings = bookings;
        this.rules = rules;
        this.idVerification = idVerification;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CycleStatus> getAvailability() {
        return cycles.findAllByOrderByLabelAsc().stream()
                .map(c -> new CycleStatus(c.getId(), c.getLabel(), c.getState()))
                .toList();
    }

    @Override
    @Transactional
    public BookingConfirmation createBooking(ResidentIdentity resident, long cycleId, ExtractedIdentity scannedCard) {
        // Rules first: a resident who may not book anyway should hear that, not be
        // told to retake their photo.
        checkRules(resident);
        if (!idVerification.matchesLogin(scannedCard, resident)) {
            throw new IdCheckFailedException(idFailureMessage(scannedCard));
        }
        return claimAndSave(resident, cycleId, IdCheck.CARD_SCAN);
    }

    @Override
    @Transactional
    public BookingConfirmation createBookingWithTypedRoll(ResidentIdentity resident, long cycleId, String typedRoll) {
        checkRules(resident);
        if (typedRoll == null || !typedRoll.strip().equalsIgnoreCase(resident.rollNumber())) {
            throw new IdCheckFailedException("That roll number is not the one on your account.");
        }
        return claimAndSave(resident, cycleId, IdCheck.TYPED_ROLL);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurrentRental> currentRental(long residentId) {
        Instant now = clock.instant();
        return bookings.findOpenBookingForResident(residentId)
                .map(b -> new CurrentRental(b.getId(), b.getCycle().getLabel(), b.getCycle().getState(),
                        b.getCreatedAt(), b.getPickedUpAt(), b.getIdCheck(), b.isOverdue(now)));
    }

    private void checkRules(ResidentIdentity resident) {
        EligibilityResult eligibility = rules.checkEligibility(resident.residentId());
        if (!eligibility.allowed()) {
            throw new BookingRejectedException(eligibility.reason().message());
        }
    }

    /** Tells apart "we could not read it" from "we read someone else's card". */
    private static String idFailureMessage(ExtractedIdentity scanned) {
        if (scanned.rollNumber() == null || scanned.confidence() < IdVerificationService.MIN_CONFIDENCE) {
            return "Could not read the roll number on your ID card. Retake the photo with the card flat, "
                    + "filling the frame, in good light.";
        }
        return "The ID card shows a different roll number from your login. Scan your own card.";
    }

    private BookingConfirmation claimAndSave(ResidentIdentity resident, long cycleId, IdCheck idCheck) {
        // The lock: a single conditional UPDATE flips the cycle AVAILABLE -> BOOKED.
        // The database runs it atomically, so when two residents race for the same
        // cycle exactly one UPDATE matches the row and the other changes nothing.
        if (cycles.claimIfAvailable(cycleId) == 0) {
            throw new BookingRejectedException("That cycle is no longer available. Pick another.");
        }

        Cycle cycle = cycles.findById(cycleId).orElseThrow();
        Instant now = clock.instant();
        Booking booking = bookings.save(
                new Booking(residents.getReferenceById(resident.residentId()), cycle, now, idCheck));
        return new BookingConfirmation(booking.getId(), cycle.getLabel(), now);
    }
}
