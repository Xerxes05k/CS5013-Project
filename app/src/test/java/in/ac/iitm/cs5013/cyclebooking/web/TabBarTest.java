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
import in.ac.iitm.cs5013.cyclebooking.booking.CurrentRental;
import in.ac.iitm.cs5013.cyclebooking.booking.CycleStatus;
import in.ac.iitm.cs5013.cyclebooking.idverify.IdVerificationService;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.IdCheck;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The tab bar from templates/fragments/layout.html, filled in by NavigationAdvice:
 * the current tab is highlighted, and My cycle shows a dot only while a rental is open.
 */
@WebMvcTest(CycleController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "ce24b128@smail.iitm.ac.in", roles = "RESIDENT")
class TabBarTest {

    private static final ResidentIdentity ME = new ResidentIdentity(1L, "CE24B128", "ce24b128@smail.iitm.ac.in");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService booking;

    @MockitoBean
    private IdVerificationService idVerification;

    @MockitoBean
    private AuthService auth;

    @MockitoBean
    private ResidentDetailsService residentDetails;

    @BeforeEach
    void setUp() {
        when(auth.currentResident(any())).thenReturn(ME);
        when(booking.getAvailability()).thenReturn(List.of(new CycleStatus(7L, "JAM-07", CycleState.AVAILABLE)));
        when(booking.currentRental(1L)).thenReturn(Optional.empty());
    }

    @Test
    void cyclesPageHighlightsTheCyclesTab() throws Exception {
        mockMvc.perform(get("/cycles"))
                .andExpect(content().string(Matchers.containsString("class=\"tab active\" href=\"/cycles\"")))
                .andExpect(content().string(Matchers.containsString("class=\"tab\" href=\"/rental\"")));
    }

    @Test
    void myCycleTabShowsADotOnlyWhileARentalIsOpen() throws Exception {
        mockMvc.perform(get("/cycles"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("tab-dot"))));

        when(booking.currentRental(1L)).thenReturn(Optional.of(new CurrentRental(42L, "JAM-07",
                CycleState.BOOKED, Instant.now(), null, IdCheck.CARD_SCAN, false)));

        mockMvc.perform(get("/cycles"))
                .andExpect(content().string(Matchers.containsString("tab-dot")));
    }

    @Test
    @WithAnonymousUser
    void loginPageHasNoTabBar() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("class=\"tabs\""))));
    }
}
