package in.ac.iitm.cs5013.cyclebooking.auth;

import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * Smail addresses of the hostel's GS (and Warden), who also get ROLE_GS: the Report
     * tab and recording repairs. A config list rather than a database column, because it
     * is one or two people who change once a year (docs/DEPLOY.md).
     */
    private final Set<String> gsSmails;

    public ResidentDetailsService(ResidentRepository residents,
            @Value("${cyclebooking.gs-smails:}") String gsSmails) {
        this.residents = residents;
        this.gsSmails = Arrays.stream(gsSmails.split(","))
                .map(s -> s.strip().toLowerCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public UserDetails loadUserByUsername(String smailAddress) throws UsernameNotFoundException {
        Resident resident = residents.findBySmailAddressIgnoreCase(smailAddress)
                .orElseThrow(() -> new UsernameNotFoundException("No resident with smail address " + smailAddress));

        return User.withUsername(resident.getSmailAddress())
                .password(resident.getPasscodeHash())
                .authorities(gsSmails.contains(resident.getSmailAddress().toLowerCase())
                        ? AuthorityUtils.createAuthorityList("ROLE_RESIDENT", "ROLE_GS")
                        : AuthorityUtils.createAuthorityList("ROLE_RESIDENT"))
                .build();
    }
}
