package in.ac.iitm.cs5013.cyclebooking.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentDetailsService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.auth.SecurityConfig;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.persistence.FlagSource;
import in.ac.iitm.cs5013.cyclebooking.reporting.MaintenanceItem;
import in.ac.iitm.cs5013.cyclebooking.reporting.ReportingService;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** The maintenance page shows each cycle's open issues with the reporter's own words (ADR 0003). */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "ce24b128@smail.iitm.ac.in", roles = "RESIDENT")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportingService reporting;

    @MockitoBean
    private BookingService booking;

    @MockitoBean
    private AuthService auth;

    @MockitoBean
    private ResidentDetailsService residentDetails;

    @BeforeEach
    void setUp() {
        when(auth.currentResident(any())).thenReturn(new ResidentIdentity(1L, "CE24B128", "ce24b128@smail.iitm.ac.in"));
    }

    @Test
    void maintenancePageGroupsIssuesByCycleWithTheReportersWords() throws Exception {
        Instant t = Instant.parse("2026-10-11T13:10:00Z");
        when(reporting.getMaintenanceList()).thenReturn(List.of(
                new MaintenanceItem(1L, "JAM-02", "guard_note", FlagSource.GUARD, "front brake loose", t),
                new MaintenanceItem(2L, "JAM-02", "chain", FlagSource.COMMENT, "chain keeps slipping off", t),
                new MaintenanceItem(3L, "JAM-05", "guard_note", FlagSource.GUARD, "bell missing", t)));

        mockMvc.perform(get("/report/maintenance"))
                .andExpect(content().string(Matchers.containsString("2 open issues")))
                .andExpect(content().string(Matchers.containsString("1 open issue<")))
                .andExpect(content().string(Matchers.containsString("front brake loose")))
                .andExpect(content().string(Matchers.containsString("Resident comment")))
                .andExpect(content().string(Matchers.containsString("chain keeps slipping off")))
                .andExpect(content().string(Matchers.containsString("class=\"subtab active\" href=\"/report/maintenance\"")));
    }

    @Test
    void maintenancePageWithNothingReportedSaysSo() throws Exception {
        when(reporting.getMaintenanceList()).thenReturn(List.of());

        mockMvc.perform(get("/report/maintenance"))
                .andExpect(content().string(Matchers.containsString("Nothing reported.")));
    }
}
