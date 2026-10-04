package in.ac.iitm.cs5013.cyclebooking.persistence;

/** How the resident proved who they were when booking. */
public enum IdCheck {
    /** OCR read their own roll number off the ID card photo. */
    CARD_SCAN,
    /**
     * The card could not be read after several tries and the resident typed their roll
     * number. The guard is asked to look at the physical card at pickup.
     */
    TYPED_ROLL
}
