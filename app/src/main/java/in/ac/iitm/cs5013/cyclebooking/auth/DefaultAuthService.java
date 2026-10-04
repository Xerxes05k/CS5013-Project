package in.ac.iitm.cs5013.cyclebooking.auth;

import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.security.Principal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultAuthService implements AuthService {

    private final ResidentRepository residents;

    DefaultAuthService(ResidentRepository residents) {
        this.residents = residents;
    }

    /** The principal's name is the smail address (ADR 0004). */
    @Override
    @Transactional(readOnly = true)
    public ResidentIdentity currentResident(Principal principal) {
        Resident r = residents.findBySmailAddressIgnoreCase(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("No resident with smail address " + principal.getName()));
        return new ResidentIdentity(r.getId(), r.getRollNumber(), r.getSmailAddress());
    }
}
