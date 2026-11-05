package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.maintenance.MaintenanceService;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.reporting.MaintenanceItem;
import in.ac.iitm.cs5013.cyclebooking.reporting.ReportingService;
import in.ac.iitm.cs5013.cyclebooking.reporting.UsageReport;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The GS/Warden usage view. It shows cycles and hours only, no resident names, so for
 * now any logged-in user may see it; a GS-only role comes with the week-5 maintenance list.
 */
@Controller
public class ReportController {

    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("d MMM, h:mm a")
            .withZone(ZoneId.of("Asia/Kolkata"));

    /** One flag as the maintenance page shows it, with its label and source in plain words. */
    record FlagRow(String issue, String reportedBy, String words, String when, long cycleId, String cycleLabel,
            Part part) {

        /** A flag about a part can be cleared with a repair; a free-text guard note can't. */
        public boolean repairable() {
            return part != null;
        }
    }

    private final ReportingService reporting;

    private final MaintenanceService maintenance;

    public ReportController(ReportingService reporting, MaintenanceService maintenance) {
        this.reporting = reporting;
        this.maintenance = maintenance;
    }

    @GetMapping("/report")
    String usage(Model model) {
        UsageReport report = reporting.getWeeklyUsageReport();
        model.addAttribute("report", report);
        model.addAttribute("from", SHOWN.format(report.from()));
        model.addAttribute("to", SHOWN.format(report.to()));
        return "report";
    }

    /** Open maintenance flags, one card per cycle, with the reporter's own words (ADR 0003). */
    @GetMapping("/report/maintenance")
    String maintenance(Model model) {
        // The service already groups by cycle; LinkedHashMap keeps that order for the page.
        Map<String, List<FlagRow>> byCycle = new LinkedHashMap<>();
        for (MaintenanceItem item : reporting.getMaintenanceList()) {
            byCycle.computeIfAbsent(item.cycleLabel(), label -> new ArrayList<>()).add(toRow(item));
        }
        model.addAttribute("byCycle", byCycle);
        return "maintenance";
    }

    private static FlagRow toRow(MaintenanceItem item) {
        String issue = item.label().equals(MaintenanceService.GUARD_NOTE_LABEL) ? "Guard note" : capitalise(item.label());
        // Exhaustive: a new FlagSource fails to compile here rather than showing the wrong reporter.
        String reportedBy = switch (item.source()) {
            case GUARD -> "Guard, at return";
            case COMMENT -> "Resident comment";
            case GS -> "GS";
        };
        Part part = Arrays.stream(Part.values()).filter(p -> p.label().equals(item.label())).findFirst().orElse(null);
        return new FlagRow(issue, reportedBy, item.words(), SHOWN.format(item.raisedAt()), item.cycleId(),
                item.cycleLabel(), part);
    }

    /**
     * The GS's Repaired button (ADR 0005): the part goes back to OK, its open flags leave
     * this list, and the cycle takes the state the fit-to-ride rule gives.
     * {@code cycleLabel} is only used to word the message.
     */
    @PostMapping("/report/maintenance/repaired")
    String repaired(@RequestParam long cycleId, @RequestParam Part part, @RequestParam String cycleLabel,
            RedirectAttributes redirect) {
        CycleState now;
        try {
            now = maintenance.recordRepair(cycleId, part, null);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", "That cycle no longer exists.");
            return "redirect:/report/maintenance";
        }
        String where = switch (now) {
            case AVAILABLE -> "It is free to book again.";
            case UNDER_REPAIR -> "It stays under repair: another critical part is still broken.";
            case BOOKED, ISSUED -> "It is with a resident; the repair is recorded.";
        };
        redirect.addFlashAttribute("notice", cycleLabel + ": " + part.label() + " marked repaired. " + where);
        return "redirect:/report/maintenance";
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
