package in.ac.iitm.cs5013.cyclebooking.persistence;

/** Who raised a maintenance flag: the guard at return, a resident's comment, or the GS. */
public enum FlagSource {
    GUARD,
    COMMENT,
    /** A repair, or a fault the GS found on inspection (ADR 0005). */
    GS
}
