package in.ac.iitm.cs5013.cyclebooking.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iitm.cs5013.cyclebooking.persistence.CycleRepository;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import in.ac.iitm.cs5013.cyclebooking.web.CycleController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Auth Module test: the availability page is not reachable without a session, and
 * the login page itself stays public.
 */
@WebMvcTest(CycleController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CycleRepository cycles;

    @MockitoBean
    private ResidentRepository residents;

    @Test
    void anonymousRequestForCyclesIsSentToLogin() throws Exception {
        mockMvc.perform(get("/cycles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "ce24b128@smail.iitm.ac.in", roles = "RESIDENT")
    void authenticatedResidentSeesTheAvailabilityPage() throws Exception {
        org.mockito.Mockito.when(cycles.findAllByOrderByLabelAsc()).thenReturn(List.of());

        mockMvc.perform(get("/cycles")).andExpect(status().isOk());
    }
}
