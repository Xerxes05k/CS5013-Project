package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * What the tab bar needs on every page (templates/fragments/layout.html). Spring runs
 * this before each controller method, so no controller has to add it itself.
 */
@ControllerAdvice
class NavigationAdvice {

    private final BookingService booking;
    private final AuthService auth;

    NavigationAdvice(BookingService booking, AuthService auth) {
        this.booking = booking;
        this.auth = auth;
    }

    /** True while the resident has a booking not yet returned: the "My cycle" tab shows a dot. */
    @ModelAttribute("hasOpenRental")
    boolean hasOpenRental(Principal principal) {
        // No principal on the login page: nobody is logged in yet.
        if (principal == null) {
            return false;
        }
        return booking.currentRental(auth.currentResident(principal).residentId()).isPresent();
    }
}
