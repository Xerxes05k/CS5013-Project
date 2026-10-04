package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingConfirmation;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingRejectedException;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.CycleStatus;
import in.ac.iitm.cs5013.cyclebooking.booking.IdCheckFailedException;
import in.ac.iitm.cs5013.cyclebooking.idverify.ExtractedIdentity;
import in.ac.iitm.cs5013.cyclebooking.idverify.IdVerificationService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Availability and booking. The controller only translates HTTP to service calls;
 * the rules, the ID check and the locking live in their modules.
 *
 * Booking is two steps: pick a cycle, then scan your ID card. Failed scans are
 * counted in the session; after {@link IdVerificationService#MAX_FAILED_SCANS} the
 * page also offers typing the roll number instead.
 */
@Controller
public class CycleController {

    static final String FAILED_SCANS = "failedIdScans";

    private final BookingService booking;
    private final IdVerificationService idVerification;
    private final AuthService auth;

    public CycleController(BookingService booking, IdVerificationService idVerification, AuthService auth) {
        this.booking = booking;
        this.idVerification = idVerification;
        this.auth = auth;
    }

    @GetMapping("/")
    String home() {
        return "redirect:/cycles";
    }

    @GetMapping("/cycles")
    String availability(Model model, Principal principal) {
        List<CycleStatus> all = booking.getAvailability();
        model.addAttribute("cycles", all);
        model.addAttribute("availableCount", all.stream().filter(CycleStatus::bookable).count());
        model.addAttribute("rental", booking.currentRental(auth.currentResident(principal).residentId()).orElse(null));
        return "cycles";
    }

    @GetMapping("/cycles/{cycleId}/verify")
    String verifyPage(@PathVariable long cycleId, Model model, HttpSession session, RedirectAttributes redirect) {
        Optional<CycleStatus> cycle = booking.getAvailability().stream()
                .filter(c -> c.id() == cycleId && c.bookable())
                .findFirst();
        if (cycle.isEmpty()) {
            redirect.addFlashAttribute("error", "That cycle is no longer available. Pick another.");
            return "redirect:/cycles";
        }
        int failed = failedScans(session);
        model.addAttribute("cycle", cycle.get());
        model.addAttribute("failedScans", failed);
        model.addAttribute("maxFailedScans", IdVerificationService.MAX_FAILED_SCANS);
        model.addAttribute("typedRollAllowed", failed >= IdVerificationService.MAX_FAILED_SCANS);
        return "verify";
    }

    /** Post-redirect-get, so refreshing the page does not try to book again. */
    @PostMapping("/cycles/{cycleId}/book")
    String book(@PathVariable long cycleId, @RequestParam(required = false) MultipartFile photo,
            Principal principal, HttpSession session, RedirectAttributes redirect) throws IOException {
        if (photo == null || photo.isEmpty()) {
            redirect.addFlashAttribute("error", "Choose or take a photo of your ID card first.");
            return "redirect:/cycles/" + cycleId + "/verify";
        }
        ResidentIdentity resident = auth.currentResident(principal);
        // The photo stays in memory for this call only and is not saved (ADR 0003).
        ExtractedIdentity scanned = idVerification.scanCard(photo.getBytes());
        return attempt(() -> booking.createBooking(resident, cycleId, scanned), cycleId, session, redirect);
    }

    @PostMapping("/cycles/{cycleId}/book-typed")
    String bookWithTypedRoll(@PathVariable long cycleId, @RequestParam String rollNumber,
            Principal principal, HttpSession session, RedirectAttributes redirect) {
        if (failedScans(session) < IdVerificationService.MAX_FAILED_SCANS) {
            redirect.addFlashAttribute("error", "Scan your ID card first.");
            return "redirect:/cycles/" + cycleId + "/verify";
        }
        ResidentIdentity resident = auth.currentResident(principal);
        return attempt(() -> booking.createBookingWithTypedRoll(resident, cycleId, rollNumber),
                cycleId, session, redirect);
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    private String attempt(Supplier<BookingConfirmation> bookIt, long cycleId,
            HttpSession session, RedirectAttributes redirect) {
        try {
            BookingConfirmation done = bookIt.get();
            session.removeAttribute(FAILED_SCANS);
            redirect.addFlashAttribute("notice", "Booked " + done.cycleLabel()
                    + ". At the stand, confirm pickup with your passcode while the guard watches.");
            return "redirect:/rental";
        } catch (IdCheckFailedException e) {
            session.setAttribute(FAILED_SCANS, failedScans(session) + 1);
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/cycles/" + cycleId + "/verify";
        } catch (BookingRejectedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/cycles";
        }
    }

    private static int failedScans(HttpSession session) {
        Object n = session.getAttribute(FAILED_SCANS);
        return n == null ? 0 : (Integer) n;
    }
}
