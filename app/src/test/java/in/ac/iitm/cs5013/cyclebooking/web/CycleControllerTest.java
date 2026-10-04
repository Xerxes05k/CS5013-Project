package in.ac.iitm.cs5013.cyclebooking.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import in.ac.iitm.cs5013.cyclebooking.auth.AuthService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentDetailsService;
import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import in.ac.iitm.cs5013.cyclebooking.auth.SecurityConfig;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingConfirmation;
import in.ac.iitm.cs5013.cyclebooking.booking.BookingService;
import in.ac.iitm.cs5013.cyclebooking.booking.CycleStatus;
import in.ac.iitm.cs5013.cyclebooking.booking.IdCheckFailedException;
import in.ac.iitm.cs5013.cyclebooking.idverify.ExtractedIdentity;
import in.ac.iitm.cs5013.cyclebooking.idverify.IdVerificationService;
import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The booking page's fallback rule: typing a roll number is only offered, and only
 * accepted, after {@link IdVerificationService#MAX_FAILED_SCANS} failed scans.
 */
@WebMvcTest(CycleController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "ce24b128@smail.iitm.ac.in", roles = "RESIDENT")
class CycleControllerTest {

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

    private final MockHttpSession session = new MockHttpSession();
    private final MockMultipartFile photo = new MockMultipartFile("photo", "card.jpg", "image/jpeg", new byte[] {1, 2, 3});

    @BeforeEach
    void setUp() {
        when(auth.currentResident(any())).thenReturn(ME);
        when(booking.getAvailability()).thenReturn(List.of(new CycleStatus(7L, "JAM-07", CycleState.AVAILABLE)));
        when(idVerification.scanCard(any())).thenReturn(ExtractedIdentity.unreadable());
        when(booking.createBooking(eq(ME), eq(7L), any()))
                .thenThrow(new IdCheckFailedException("Could not read the roll number on your ID card."));
    }

    private void failOneScan() throws Exception {
        mockMvc.perform(multipart("/cycles/7/book").file(photo).session(session).with(csrf()))
                .andExpect(redirectedUrl("/cycles/7/verify"));
    }

    @Test
    void typedRollIsNotOfferedBeforeEnoughFailedScans() throws Exception {
        for (int i = 1; i < IdVerificationService.MAX_FAILED_SCANS; i++) {
            failOneScan();
        }

        mockMvc.perform(get("/cycles/7/verify").session(session))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Book with roll number"))));
        mockMvc.perform(post("/cycles/7/book-typed").param("rollNumber", "CE24B128").session(session).with(csrf()))
                .andExpect(redirectedUrl("/cycles/7/verify"));
        verify(booking, never()).createBookingWithTypedRoll(any(), anyLong(), any());
    }

    @Test
    void typedRollIsOfferedAndAcceptedAfterMaxFailedScans() throws Exception {
        for (int i = 0; i < IdVerificationService.MAX_FAILED_SCANS; i++) {
            failOneScan();
        }
        when(booking.createBookingWithTypedRoll(ME, 7L, "CE24B128"))
                .thenReturn(new BookingConfirmation(99L, "JAM-07", Instant.now()));

        mockMvc.perform(get("/cycles/7/verify").session(session))
                .andExpect(content().string(Matchers.containsString("Book with roll number")));
        mockMvc.perform(post("/cycles/7/book-typed").param("rollNumber", "CE24B128").session(session).with(csrf()))
                .andExpect(redirectedUrl("/rental"));
        verify(booking).createBookingWithTypedRoll(ME, 7L, "CE24B128");
    }
}
