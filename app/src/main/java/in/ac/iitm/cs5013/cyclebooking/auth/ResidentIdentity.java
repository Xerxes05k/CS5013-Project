package in.ac.iitm.cs5013.cyclebooking.auth;

/** The logged-in resident, as other modules see them. */
public record ResidentIdentity(long residentId, String rollNumber, String smailAddress) {
}
