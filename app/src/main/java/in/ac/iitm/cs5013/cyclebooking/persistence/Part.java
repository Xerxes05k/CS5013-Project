package in.ac.iitm.cs5013.cyclebooking.persistence;

import java.util.Locale;

/**
 * The 8 parts every cycle has (ADR 0005). They are the classifier's labels, so a label
 * read from a resident's comment maps straight to a part.
 */
public enum Part {
    TYRE(true),
    BRAKES(true),
    CHAIN(true),
    SEAT(true),
    PEDAL(true),
    HANDLEBAR(true),
    LOCK(false),
    BELL(false);

    private final boolean critical;

    Part(boolean critical) {
        this.critical = critical;
    }

    /**
     * True if the cycle is unsafe or can't be ridden while this part is broken, which
     * makes it UNDER_REPAIR. A broken lock or bell only warns the next resident.
     */
    public boolean critical() {
        return critical;
    }

    /** The classifier label and maintenance-flag label for this part, e.g. "brakes". */
    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
