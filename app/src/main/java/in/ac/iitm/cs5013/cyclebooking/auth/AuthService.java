package in.ac.iitm.cs5013.cyclebooking.auth;

import java.security.Principal;

/** Auth Module: who is the resident behind this logged-in request. */
public interface AuthService {

    ResidentIdentity currentResident(Principal principal);
}
