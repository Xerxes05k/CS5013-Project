package in.ac.iitm.cs5013.cyclebooking.persistence;

/** Lifecycle of a physical cycle. See docs/ARCHITECTURE.md for the state machine. */
public enum CycleState {
    AVAILABLE,
    BOOKED,
    ISSUED,
    UNDER_REPAIR
}
