package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingConfirmation;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingRejectedException;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.CycleStatus;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Availability and booking. The controller only translates HTTP to service calls;
 * the rules and the locking live in the Rule Engine and Booking Module.
 */
@Controller
public class CycleController {

    private final BookingService booking;
    private final AuthService auth;

    public CycleController(BookingService booking, AuthService auth) {
        this.booking = booking;
        this.auth = auth;
    }

    @GetMapping("/cycles")
    String availability(Model model) {
        List<CycleStatus> all = booking.getAvailability();
        model.addAttribute("cycles", all);
        model.addAttribute("availableCount", all.stream().filter(CycleStatus::bookable).count());
        return "cycles";
    }

    /** Post-redirect-get, so refreshing the page does not try to book again. */
    @PostMapping("/cycles/{cycleId}/book")
    String book(@PathVariable long cycleId, Principal principal, RedirectAttributes redirect) {
        long residentId = auth.currentResident(principal).residentId();
        try {
            BookingConfirmation done = booking.createBooking(residentId, cycleId);
            redirect.addFlashAttribute("notice", "Booked " + done.cycleLabel() + ". Show your passcode to the guard at pickup.");
        } catch (BookingRejectedException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cycles";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
