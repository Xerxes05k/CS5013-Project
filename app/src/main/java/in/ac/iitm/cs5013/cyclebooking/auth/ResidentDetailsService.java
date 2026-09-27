package in.ac.iitm.cs5013.cyclebooking.auth;

import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a resident by their institute (smail) address for login.
 *
 * The username is always the smail address, so that when institute OAuth2 becomes
 * available (ADR 0004) the same identity key carries over and existing bookings
 * still resolve to the same resident.
 */
@Service
public class ResidentDetailsService implements UserDetailsService {

    private final ResidentRepository residents;

    public ResidentDetailsService(ResidentRepository residents) {
        this.residents = residents;
    }

    @Override
    public UserDetails loadUserByUsername(String smailAddress) throws UsernameNotFoundException {
        Resident resident = residents.findBySmailAddressIgnoreCase(smailAddress)
                .orElseThrow(() -> new UsernameNotFoundException("No resident with smail address " + smailAddress));

        return User.withUsername(resident.getSmailAddress())
                .password(resident.getPasscodeHash())
                .authorities(AuthorityUtils.createAuthorityList("ROLE_RESIDENT"))
                .build();
    }
}
