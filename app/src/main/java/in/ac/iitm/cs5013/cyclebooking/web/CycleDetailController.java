package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.CycleStatus;
import in.ac.iitm.cs5013.cyclebooking.maintenance.MaintenanceService;
import in.ac.iitm.cs5013.cyclebooking.maintenance.PartStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Cycle detail: the 8 parts and their condition (ADR 0005). These are the properties
 * that decide whether the cycle can be booked, so residents and the GS can see why.
 */
@Controller
public class CycleDetailController {

    /** One part as the grid shows it. {@code condition} is also the CSS class suffix. */
    record PartRow(String name, boolean critical, String condition, String shown, String note) {
    }

    private final BookingService booking;
    private final MaintenanceService maintenance;

    public CycleDetailController(BookingService booking, MaintenanceService maintenance) {
        this.booking = booking;
        this.maintenance = maintenance;
    }

    @GetMapping("/cycles/{cycleId}")
    String detail(@PathVariable long cycleId, Model model, RedirectAttributes redirect) {
        Optional<CycleStatus> cycle = booking.getAvailability().stream().filter(c -> c.id() == cycleId).findFirst();
        if (cycle.isEmpty()) {
            redirect.addFlashAttribute("error", "No such cycle.");
            return "redirect:/cycles";
        }
        List<PartRow> parts = maintenance.partsOf(cycleId).stream().map(CycleDetailController::toRow).toList();
        model.addAttribute("cycle", cycle.get());
        model.addAttribute("parts", parts);
        return "cycle";
    }

    private static PartRow toRow(PartStatus p) {
        String name = p.part().label();
        String shown = switch (p.condition()) {
            case OK -> "✓ OK";
            case REPORTED -> "⚠ Reported";
            case BROKEN -> "✕ Broken";
        };
        return new PartRow(Character.toUpperCase(name.charAt(0)) + name.substring(1), p.critical(),
                p.condition().name(), shown, p.note());
    }
}
