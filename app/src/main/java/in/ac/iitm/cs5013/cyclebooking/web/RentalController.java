package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.CurrentRental;
import in.ac.iitm.cs5013.cyclebooking.maintenance.CommentRejectedException;
import in.ac.iitm.cs5013.cyclebooking.maintenance.MaintenanceService;
import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.transaction.TransactionRejectedException;
import in.ac.iitm.cs5013.cyclebooking.transaction.TransactionService;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * "My cycle": the resident's open booking, the pickup and return steps done at the
 * stand in front of the guard, and the comment box after the return. The booking id
 * is never taken from the form: it is the logged-in resident's own open booking, or,
 * for the comment, the booking they just returned, kept in their session.
 */
@Controller
public class RentalController {

    private static final ZoneId HOSTEL_TIME = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a");

    /** Session key for the rental just returned, which the comment box is about. */
    static final String RETURNED = "returnedRental";

    /** The booking the comment box is for. Kept in the session, never sent to the browser. */
    record ReturnedRental(long bookingId, String cycleLabel) implements Serializable {
    }

    /**
     * The big green panel shown after a pickup or return succeeds. The guard has no
     * device and reads this off the resident's phone, so it says what the guard should do.
     */
    record GuardConfirmation(String title, String cycleLabel, String detail) implements Serializable {
    }

    private final BookingService booking;
    private final TransactionService transactions;
    private final MaintenanceService maintenance;
    private final AuthService auth;

    public RentalController(BookingService booking, TransactionService transactions,
            MaintenanceService maintenance, AuthService auth) {
        this.booking = booking;
        this.transactions = transactions;
        this.maintenance = maintenance;
        this.auth = auth;
    }

    @GetMapping("/rental")
    String myCycle(Principal principal, Model model, RedirectAttributes redirect) {
        Optional<CurrentRental> rental = current(principal);
        if (rental.isEmpty()) {
            // Reached from the "My cycle" tab with nothing booked: say so rather than
            // silently showing the cycle list.
            redirect.addFlashAttribute("notice", "You have no cycle booked. Pick a free one below.");
            return "redirect:/cycles";
        }
        CurrentRental r = rental.get();
        model.addAttribute("rental", r);
        model.addAttribute("bookedAt", show(r.createdAt()));
        if (!r.awaitingPickup()) {
            model.addAttribute("pickedUpAt", show(r.pickedUpAt()));
            model.addAttribute("dueBy", show(r.pickedUpAt().plus(Booking.MAX_HOLD)));
            // The return form's checklist: one box per part, ticked if the guard found it broken.
            model.addAttribute("parts", Part.values());
        }
        return "rental";
    }

    @PostMapping("/rental/pickup")
    String pickup(@RequestParam String passcode, Principal principal, RedirectAttributes redirect) {
        Optional<CurrentRental> rental = current(principal);
        if (rental.isEmpty()) {
            redirect.addFlashAttribute("error", "You have no cycle booked.");
            return "redirect:/cycles";
        }
        try {
            transactions.confirmPickup(rental.get().bookingId(), passcode);
        } catch (TransactionRejectedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/rental";
        }
        // Read the booking again so the due time comes from the pickup time just saved.
        String detail = current(principal)
                .filter(r -> r.pickedUpAt() != null)
                .map(r -> "Guard: hand over the cycle. Due back by " + show(r.pickedUpAt().plus(Booking.MAX_HOLD)) + ".")
                .orElse("Guard: hand over the cycle.");
        redirect.addFlashAttribute("confirmed",
                new GuardConfirmation("Pickup confirmed", rental.get().cycleLabel(), detail));
        return "redirect:/rental";
    }

    @PostMapping("/rental/return")
    String giveBack(@RequestParam String passcode, @RequestParam(defaultValue = "") String guardNote,
            @RequestParam(required = false) Set<Part> brokenParts,
            Principal principal, HttpSession session, RedirectAttributes redirect) {
        Optional<CurrentRental> rental = current(principal);
        if (rental.isEmpty()) {
            redirect.addFlashAttribute("error", "You have no cycle booked.");
            return "redirect:/cycles";
        }
        CurrentRental r = rental.get();
        try {
            // One "brokenParts" checkbox per part on the return form; none ticked sends none.
            transactions.confirmReturn(r.bookingId(), passcode, brokenParts == null ? Set.of() : brokenParts,
                    guardNote);
        } catch (TransactionRejectedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/rental";
        }
        session.setAttribute(RETURNED, new ReturnedRental(r.bookingId(), r.cycleLabel()));
        redirect.addFlashAttribute("confirmed",
                new GuardConfirmation("Return confirmed", r.cycleLabel(), "Guard: take the cycle back. Thanks!"));
        return "redirect:/rental/comment";
    }

    @GetMapping("/rental/comment")
    String commentBox(HttpSession session, Model model) {
        if (!(session.getAttribute(RETURNED) instanceof ReturnedRental returned)) {
            return "redirect:/cycles";
        }
        model.addAttribute("returned", returned);
        model.addAttribute("maxLength", MaintenanceService.MAX_COMMENT_LENGTH);
        return "comment";
    }

    @PostMapping("/rental/comment")
    String comment(@RequestParam(defaultValue = "") String text, HttpSession session, RedirectAttributes redirect) {
        if (!(session.getAttribute(RETURNED) instanceof ReturnedRental returned)) {
            return "redirect:/cycles";
        }
        try {
            maintenance.submitComment(returned.bookingId(), text);
        } catch (CommentRejectedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/rental/comment";
        }
        session.removeAttribute(RETURNED);
        redirect.addFlashAttribute("notice", "Thanks! Your comment on " + returned.cycleLabel() + " goes to the GS.");
        return "redirect:/cycles";
    }

    @PostMapping("/rental/comment/skip")
    String skipComment(HttpSession session) {
        session.removeAttribute(RETURNED);
        return "redirect:/cycles";
    }

    private Optional<CurrentRental> current(Principal principal) {
        return booking.currentRental(auth.currentResident(principal).residentId());
    }

    private static String show(Instant t) {
        return SHOWN.format(t.atZone(HOSTEL_TIME));
    }
}
