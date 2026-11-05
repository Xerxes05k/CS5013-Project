package in.ac.iitm.cs5013.cyclebooking.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Only the smail addresses listed in cyclebooking.gs-smails get the GS role. */
class ResidentDetailsServiceTest {

    private final ResidentRepository residents = mock(ResidentRepository.class);

    private UserDetails load(String smail, String gsSmails) {
        when(residents.findBySmailAddressIgnoreCase(smail)).thenReturn(Optional.of(
                new Resident("CE24B001", "Test", "Jamuna", smail, "hash")));
        return new ResidentDetailsService(residents, gsSmails).loadUserByUsername(smail);
    }

    @Test
    void listedSmailGetsTheGsRoleAsWellAsResident() {
        UserDetails gs = load("gs@smail.iitm.ac.in", " other@smail.iitm.ac.in, GS@smail.iitm.ac.in ");

        assertThat(gs.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_RESIDENT", "ROLE_GS");
    }

    @Test
    void residentNotListedIsOnlyAResident() {
        assertThat(load("ce24b001@smail.iitm.ac.in", "gs@smail.iitm.ac.in").getAuthorities())
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_RESIDENT");
        assertThat(load("ce24b001@smail.iitm.ac.in", "").getAuthorities())
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_RESIDENT");
    }
}
