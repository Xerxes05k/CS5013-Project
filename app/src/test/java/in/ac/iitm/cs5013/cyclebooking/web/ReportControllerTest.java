package in.ac.iitm.cs5013.cyclebooking.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentDetailsService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.auth.SecurityConfig;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.maintenance.MaintenanceService;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.FlagSource;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
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
@WithMockUser(username = "ce24b128@smail.iitm.ac.in", roles = {"RESIDENT", "GS"})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportingService reporting;

    @MockitoBean
    private MaintenanceService maintenance;

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
                new MaintenanceItem(1L, 2L, "JAM-02", MaintenanceService.GUARD_NOTE_LABEL, FlagSource.GUARD, "front brake loose", t),
                new MaintenanceItem(2L, 2L, "JAM-02", "chain", FlagSource.COMMENT, "chain keeps slipping off", t),
                new MaintenanceItem(3L, 5L, "JAM-05", MaintenanceService.GUARD_NOTE_LABEL, FlagSource.GUARD, "bell missing", t)));

        mockMvc.perform(get("/report/maintenance"))
                .andExpect(content().string(Matchers.containsString("2 open issues")))
                .andExpect(content().string(Matchers.containsString("1 open issue<")))
                .andExpect(content().string(Matchers.containsString("front brake loose")))
                .andExpect(content().string(Matchers.containsString("Resident comment")))
                .andExpect(content().string(Matchers.containsString("chain keeps slipping off")))
                .andExpect(content().string(Matchers.containsString("class=\"subtab active\" href=\"/report/maintenance\"")));
    }

    @Test
    void flagRaisedByTheGsIsShownAsTheGsNotAsAResident() throws Exception {
        when(reporting.getMaintenanceList()).thenReturn(List.of(new MaintenanceItem(4L, 3L, "JAM-03", "tyre",
                FlagSource.GS, "flat on inspection", Instant.parse("2026-10-28T05:00:00Z"))));

        mockMvc.perform(get("/report/maintenance"))
                .andExpect(content().string(Matchers.containsString("<span>GS</span>")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Resident comment"))));
    }

    @Test
    void maintenancePageWithNothingReportedSaysSo() throws Exception {
        when(reporting.getMaintenanceList()).thenReturn(List.of());

        mockMvc.perform(get("/report/maintenance"))
                .andExpect(content().string(Matchers.containsString("Nothing reported.")));
    }

    @Test
    void partFlagHasARepairedButtonButAFreeTextGuardNoteDoesNot() throws Exception {
        Instant t = Instant.parse("2026-11-05T05:00:00Z");
        when(reporting.getMaintenanceList()).thenReturn(List.of(
                new MaintenanceItem(1L, 5L, "JAM-05", "brakes", FlagSource.GUARD, "front brake loose", t),
                new MaintenanceItem(2L, 5L, "JAM-05", MaintenanceService.GUARD_NOTE_LABEL, FlagSource.GUARD, "dirty", t)));

        String page = mockMvc.perform(get("/report/maintenance")).andReturn().getResponse().getContentAsString();

        assertThat(page.split("name=\"part\"", -1)).hasSize(2);
        assertThat(page).contains("value=\"BRAKES\"");
    }

    @Test
    void repairedRecordsTheRepairAndSaysWhereTheCycleIsNow() throws Exception {
        when(maintenance.recordRepair(5L, Part.BRAKES, null)).thenReturn(CycleState.AVAILABLE);

        mockMvc.perform(post("/report/maintenance/repaired").param("cycleId", "5").param("part", "BRAKES")
                        .param("cycleLabel", "JAM-05").with(csrf()))
                .andExpect(redirectedUrl("/report/maintenance"))
                .andExpect(flash().attribute("notice", "JAM-05: brakes marked repaired. It is free to book again."));
        verify(maintenance).recordRepair(5L, Part.BRAKES, null);
    }

    @Test
    @WithMockUser(username = "ce24b001@smail.iitm.ac.in", roles = "RESIDENT")
    void residentWhoIsNotTheGsCannotSeeReportsOrRecordRepairs() throws Exception {
        mockMvc.perform(get("/report")).andExpect(status().isForbidden());
        mockMvc.perform(get("/report/maintenance")).andExpect(status().isForbidden());
        mockMvc.perform(post("/report/maintenance/repaired").param("cycleId", "5").param("part", "BRAKES")
                        .param("cycleLabel", "JAM-05").with(csrf()))
                .andExpect(status().isForbidden());
        verify(maintenance, never()).recordRepair(anyLong(), any(), any());
    }
}
