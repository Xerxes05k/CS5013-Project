package in.ac.iitm.cs5013.cyclebooking.web;

import in.ac.iitm.cs5013.cyclebooking.reporting.ReportingService;
import in.ac.iitm.cs5013.cyclebooking.reporting.UsageReport;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
}
