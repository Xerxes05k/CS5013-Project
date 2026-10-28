package in.ac.iitm.cs5013.cyclebooking.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentDetailsService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.auth.SecurityConfig;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.CurrentRental;
import in.ac.iitm.cs5013.cyclebooking.maintenance.MaintenanceService;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import in.ac.iitm.cs5013.cyclebooking.persistence.IdCheck;
import in.ac.iitm.cs5013.cyclebooking.persistence.Part;
import in.ac.iitm.cs5013.cyclebooking.transaction.TransactionService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The comment box only ever comments on the booking the resident has just returned:
 * the id comes from the session set by the return, never from the form.
 */
@WebMvcTest(RentalController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "ce24b128@smail.iitm.ac.in", roles = "RESIDENT")
class RentalControllerTest {

    private static final ResidentIdentity ME = new ResidentIdentity(1L, "CE24B128", "ce24b128@smail.iitm.ac.in");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService booking;

    @MockitoBean
    private TransactionService transactions;

    @MockitoBean
    private MaintenanceService maintenance;

    @MockitoBean
    private AuthService auth;

    @MockitoBean
    private ResidentDetailsService residentDetails;

    private final MockHttpSession session = new MockHttpSession();

    @BeforeEach
    void setUp() {
        when(auth.currentResident(any())).thenReturn(ME);
        when(booking.currentRental(1L)).thenReturn(Optional.of(new CurrentRental(42L, "JAM-02",
                CycleState.ISSUED, Instant.now(), Instant.now(), IdCheck.CARD_SCAN, false)));
    }

    @Test
    void returnPassesGuardNoteAndOpensCommentBox() throws Exception {
        mockMvc.perform(post("/rental/return").param("passcode", "cycle1234").param("guardNote", "bell missing")
                        .session(session).with(csrf()))
                .andExpect(redirectedUrl("/rental/comment"));
        verify(transactions).confirmReturn(42L, "cycle1234", Set.of(), "bell missing");

        mockMvc.perform(get("/rental/comment").session(session))
                .andExpect(content().string(Matchers.containsString("How was <span>JAM-02</span>?")));
    }

    @Test
    void commentGoesToTheBookingJustReturned() throws Exception {
        mockMvc.perform(post("/rental/return").param("passcode", "cycle1234").session(session).with(csrf()));

        mockMvc.perform(post("/rental/comment").param("text", "seat is loose").session(session).with(csrf()))
                .andExpect(redirectedUrl("/cycles"));
        verify(maintenance).submitComment(42L, "seat is loose");

        // The box closes after one comment.
        mockMvc.perform(get("/rental/comment").session(session)).andExpect(redirectedUrl("/cycles"));
    }

    @Test
    void commentWithoutAReturnIsIgnored() throws Exception {
        mockMvc.perform(post("/rental/comment").param("text", "seat is loose").param("bookingId", "7")
                        .session(session).with(csrf()))
                .andExpect(redirectedUrl("/cycles"));
        verify(maintenance, never()).submitComment(anyLong(), anyString());
    }

    @Test
    void myCycleTabWithNothingBookedSaysSoOnTheCycleList() throws Exception {
        when(booking.currentRental(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/rental"))
                .andExpect(redirectedUrl("/cycles"))
                .andExpect(flash().attribute("notice", "You have no cycle booked. Pick a free one below."));
    }

    @Test
    void pickupShowsTheGuardWhatToDoAndWhenItIsDue() throws Exception {
        RentalController.GuardConfirmation shown = (RentalController.GuardConfirmation) mockMvc
                .perform(post("/rental/pickup").param("passcode", "cycle1234").with(csrf()))
                .andExpect(redirectedUrl("/rental"))
                .andReturn().getFlashMap().get("confirmed");

        verify(transactions).confirmPickup(42L, "cycle1234");
        assertThat(shown.title()).isEqualTo("Pickup confirmed");
        assertThat(shown.cycleLabel()).isEqualTo("JAM-02");
        assertThat(shown.detail()).startsWith("Guard: hand over the cycle. Due back by ");
    }

    @Test
    void returnPassesThePartsTheGuardFoundBroken() throws Exception {
        mockMvc.perform(post("/rental/return").param("passcode", "cycle1234")
                        .param("brokenParts", "BRAKES", "BELL").session(session).with(csrf()))
                .andExpect(redirectedUrl("/rental/comment"));

        verify(transactions).confirmReturn(42L, "cycle1234", Set.of(Part.BRAKES, Part.BELL), "");
    }

    @Test
    void returnShowsTheGuardAReturnConfirmation() throws Exception {
        mockMvc.perform(post("/rental/return").param("passcode", "cycle1234").session(session).with(csrf()))
                .andExpect(flash().attribute("confirmed", new RentalController.GuardConfirmation(
                        "Return confirmed", "JAM-02", "Guard: take the cycle back. Thanks!")));
    }

    @Test
    void confirmationPanelIsShownOnlyRightAfterTheStep() throws Exception {
        mockMvc.perform(get("/rental").flashAttr("confirmed",
                        new RentalController.GuardConfirmation("Pickup confirmed", "JAM-02", "Guard: hand over the cycle.")))
                .andExpect(content().string(Matchers.containsString("class=\"confirmed\"")))
                .andExpect(content().string(Matchers.containsString("Guard: hand over the cycle.")));

        mockMvc.perform(get("/rental"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("class=\"confirmed\""))));
    }
}
