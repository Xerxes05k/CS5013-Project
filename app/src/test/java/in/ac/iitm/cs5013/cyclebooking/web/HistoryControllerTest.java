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
import in.ac.iitm.cs5013.cyclebooking.booking.HistoryEntry;
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

/** The History tab shows only the logged-in resident's rentals, with hours and late returns marked. */
@WebMvcTest(HistoryController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "ce24b128@smail.iitm.ac.in", roles = "RESIDENT")
class HistoryControllerTest {

    private static final ResidentIdentity ME = new ResidentIdentity(1L, "CE24B128", "ce24b128@smail.iitm.ac.in");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService booking;

    @MockitoBean
    private AuthService auth;

    @MockitoBean
    private ResidentDetailsService residentDetails;

    @BeforeEach
    void setUp() {
        when(auth.currentResident(any())).thenReturn(ME);
    }

    @Test
    void historyShowsEachRentalWithHoursAndMarksLateReturns() throws Exception {
        Instant sun = Instant.parse("2026-10-11T08:00:00Z");
        Instant thu = Instant.parse("2026-10-08T08:00:00Z");
        when(booking.history(1L)).thenReturn(List.of(
                new HistoryEntry("JAM-04", sun, sun, sun.plusSeconds(3 * 3600 + 1800), 3.5, false),
                new HistoryEntry("JAM-01", thu, thu, thu.plusSeconds(26 * 3600), 26.0, true)));

        mockMvc.perform(get("/history"))
                .andExpect(content().string(Matchers.containsString("Sun 11 Oct · 3.5 h")))
                .andExpect(content().string(Matchers.containsString("Thu 8 Oct · 26.0 h")))
                .andExpect(content().string(Matchers.containsString("Held over 24 h")))
                .andExpect(content().string(Matchers.containsString("class=\"tab active\" href=\"/history\"")));
    }

    @Test
    void historyWithNoRentalsSaysWhereToBook() throws Exception {
        when(booking.history(1L)).thenReturn(List.of());

        mockMvc.perform(get("/history"))
                .andExpect(content().string(Matchers.containsString("No rentals yet. Book one from the Cycles tab.")));
    }
}
