package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.CurrentRental;
import in.ac.iitm.cs5013.cyclebooking.persistence.Booking;
import in.ac.iitm.cs5013.cyclebooking.transaction.TransactionRejectedException;
import in.ac.iitm.cs5013.cyclebooking.transaction.TransactionService;
import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.LongConsumer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * "My cycle": the resident's open booking, and the pickup and return steps done at
 * the stand in front of the guard. The booking id is never taken from the form; it
 * is always the logged-in resident's own open booking.
 */
@Controller
public class RentalController {

    private static final ZoneId HOSTEL_TIME = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a");

    private final BookingService booking;
    private final TransactionService transactions;
    private final AuthService auth;

    public RentalController(BookingService booking, TransactionService transactions, AuthService auth) {
        this.booking = booking;
        this.transactions = transactions;
        this.auth = auth;
    }

    @GetMapping("/rental")
    String myCycle(Principal principal, Model model) {
        Optional<CurrentRental> rental = current(principal);
        if (rental.isEmpty()) {
            return "redirect:/cycles";
        }
        CurrentRental r = rental.get();
        model.addAttribute("rental", r);
        model.addAttribute("bookedAt", show(r.createdAt()));
        if (!r.awaitingPickup()) {
            model.addAttribute("pickedUpAt", show(r.pickedUpAt()));
            model.addAttribute("dueBy", show(r.pickedUpAt().plus(Booking.MAX_HOLD)));
        }
        return "rental";
    }

    @PostMapping("/rental/pickup")
    String pickup(@RequestParam String passcode, Principal principal, RedirectAttributes redirect) {
        return step(principal, redirect, id -> transactions.confirmPickup(id, passcode),
                "Pickup confirmed. The guard can hand over the cycle. Return it within 24 hours.", "/rental");
    }

    @PostMapping("/rental/return")
    String giveBack(@RequestParam String passcode, Principal principal, RedirectAttributes redirect) {
        return step(principal, redirect, id -> transactions.confirmReturn(id, passcode),
                "Return confirmed. Thanks!", "/cycles");
    }

    /** Runs a pickup or return on the resident's own open booking and redirects with the outcome. */
    private String step(Principal principal, RedirectAttributes redirect, LongConsumer action,
            String successNotice, String successPage) {
        Optional<CurrentRental> rental = current(principal);
        if (rental.isEmpty()) {
            redirect.addFlashAttribute("error", "You have no cycle booked.");
            return "redirect:/cycles";
        }
        try {
            action.accept(rental.get().bookingId());
        } catch (TransactionRejectedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/rental";
        }
        redirect.addFlashAttribute("notice", successNotice);
        return "redirect:" + successPage;
    }

    private Optional<CurrentRental> current(Principal principal) {
        return booking.currentRental(auth.currentResident(principal).residentId());
    }

    private static String show(Instant t) {
        return SHOWN.format(t.atZone(HOSTEL_TIME));
    }
}
