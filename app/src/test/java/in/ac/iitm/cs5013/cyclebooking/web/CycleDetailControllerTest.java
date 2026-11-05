package in.ac.iitm.cs5013.cyclebooking.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentDetailsService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.auth.SecurityConfig;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.CycleStatus;
import in.ac.iitm.cs5013.cyclebooking.maintenance.MaintenanceService;
import in.ac.iitm.cs5013.cyclebooking.maintenance.PartStatus;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.persistence.PartCondition;
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

/** The cycle detail page shows each part's condition, the properties behind the cycle's state. */
@WebMvcTest(CycleDetailController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "ce24b128@smail.iitm.ac.in", roles = "RESIDENT")
class CycleDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService booking;

    @MockitoBean
    private MaintenanceService maintenance;

    @MockitoBean
    private AuthService auth;

    @MockitoBean
    private ResidentDetailsService residentDetails;

    @BeforeEach
    void setUp() {
        when(auth.currentResident(any())).thenReturn(new ResidentIdentity(1L, "CE24B128", "ce24b128@smail.iitm.ac.in"));
        when(booking.getAvailability()).thenReturn(
                List.of(new CycleStatus(5L, "JAM-05", CycleState.UNDER_REPAIR, List.of(Part.BRAKES, Part.BELL))));
    }

    @Test
    void detailShowsEveryPartWithItsConditionInWords() throws Exception {
        Instant t = Instant.parse("2026-10-28T05:00:00Z");
        when(maintenance.partsOf(5L)).thenReturn(List.of(
                new PartStatus(Part.BRAKES, PartCondition.BROKEN, t, "front brake loose"),
                new PartStatus(Part.BELL, PartCondition.REPORTED, t, null),
                new PartStatus(Part.TYRE, PartCondition.OK, t, null)));

        mockMvc.perform(get("/cycles/5"))
                .andExpect(content().string(Matchers.containsString("part-tile part-BROKEN")))
                .andExpect(content().string(Matchers.containsString("✕ Broken")))
                .andExpect(content().string(Matchers.containsString("front brake loose")))
                .andExpect(content().string(Matchers.containsString("⚠ Reported")))
                .andExpect(content().string(Matchers.containsString("✓ OK")))
                .andExpect(content().string(Matchers.containsString("chip-UNDER_REPAIR")));
    }

    @Test
    void unknownCycleGoesBackToTheListWithAReason() throws Exception {
        mockMvc.perform(get("/cycles/99"))
                .andExpect(redirectedUrl("/cycles"))
                .andExpect(flash().attribute("error", "No such cycle."));
    }
}
