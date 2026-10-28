package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.reporting.MaintenanceItem;
import in.ac.iitm.cs5013.cyclebooking.reporting.ReportingService;
import in.ac.iitm.cs5013.cyclebooking.reporting.UsageReport;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The GS/Warden usage view. It shows cycles and hours only, no resident names, so for
 * now any logged-in user may see it; a GS-only role comes with the week-5 maintenance list.
 */
@Controller
public class ReportController {

    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("d MMM, h:mm a")
            .withZone(ZoneId.of("Asia/Kolkata"));

    /** One flag as the maintenance page shows it, with its label and source in plain words. */
    record FlagRow(String issue, String reportedBy, String words, String when) {
    }

    private final ReportingService reporting;

    public ReportController(ReportingService reporting) {
        this.reporting = reporting;
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
        String issue = item.label().equals("guard_note") ? "Guard note" : capitalise(item.label());
        // Exhaustive: a new FlagSource fails to compile here rather than showing the wrong reporter.
        String reportedBy = switch (item.source()) {
            case GUARD -> "Guard, at return";
            case COMMENT -> "Resident comment";
            case GS -> "GS";
        };
        return new FlagRow(issue, reportedBy, item.words(), SHOWN.format(item.raisedAt()));
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
