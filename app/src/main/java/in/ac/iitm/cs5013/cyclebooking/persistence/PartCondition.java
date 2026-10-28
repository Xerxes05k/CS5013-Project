package in.ac.iitm.cs5013.cyclebooking.persistence;

/** The current condition of one part of one cycle (ADR 0005). */
public enum PartCondition {
    OK,
    /** Someone said something is wrong, nobody has confirmed it. Set by the classifier. */
    REPORTED,
    /** The guard or the GS saw it broken. */
    BROKEN
}
