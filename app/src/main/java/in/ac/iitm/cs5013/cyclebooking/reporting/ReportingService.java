package in.ac.iitm.cs5013.cyclebooking.reporting;

import java.time.Instant;

/** Reporting Module: the GS/Warden view of how the cycle pool is used. */
public interface ReportingService {

    /** Usage over the last 7 days, ending now. */
    UsageReport getWeeklyUsageReport();

    /** Usage over [from, to). Rentals that straddle an edge count only their hours inside it. */
    UsageReport getUsageReport(Instant from, Instant to);
}
