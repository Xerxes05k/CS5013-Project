package in.ac.iitm.cs5013.cyclebooking.booking;

import in.ac.iitm.cs5013.cyclebooking.persistence.CycleState;

/** What the availability page needs to know about one cycle. */
public record CycleStatus(long id, String label, CycleState state) {

    public boolean bookable() {
        return state == CycleState.AVAILABLE;
    }
}
