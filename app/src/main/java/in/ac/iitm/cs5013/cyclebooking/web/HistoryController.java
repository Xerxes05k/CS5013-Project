package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.HistoryEntry;
import java.security.Principal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The History tab: the logged-in resident's own rentals, newest first. */
@Controller
public class HistoryController {

    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("EEE d MMM")
            .withZone(ZoneId.of("Asia/Kolkata"));

    /** One row as the page shows it, with the date and hours already formatted. */
    record HistoryRow(String cycleLabel, String bookedOn, String hours, String status, boolean late) {
    }

    private final BookingService booking;
    private final AuthService auth;

    public HistoryController(BookingService booking, AuthService auth) {
        this.booking = booking;
        this.auth = auth;
    }

    @GetMapping("/history")
    String history(Principal principal, Model model) {
        List<HistoryRow> rows = booking.history(auth.currentResident(principal).residentId()).stream()
                .map(HistoryController::toRow)
                .toList();
        model.addAttribute("rows", rows);
        return "history";
    }

    private static HistoryRow toRow(HistoryEntry e) {
        String hours = e.pickedUpAt() == null ? "not picked up" : String.format("%.1f h", e.hours());
        return new HistoryRow(e.cycleLabel(), SHOWN.format(e.bookedAt()), hours, e.status(), e.late());
    }
}
