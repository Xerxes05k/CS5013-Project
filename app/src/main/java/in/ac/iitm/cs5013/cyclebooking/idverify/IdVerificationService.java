package in.ac.iitm.cs5013.cyclebooking.idverify;

import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;

/**
 * ID Verification Module: read the resident's ID card photo and check it belongs to
 * the logged-in resident. The photo is never stored (ADR 0003).
 */
public interface IdVerificationService {

    /** Below this OCR confidence the resident is asked to retake the photo. */
    float MIN_CONFIDENCE = 70f;

    /** After this many failed scans the resident may type their roll number instead. */
    int MAX_FAILED_SCANS = 3;

    /**
     * Never throws for a bad photo: an unreadable or non-image upload comes back as
     * {@link ExtractedIdentity#unreadable()}, which fails {@link #matchesLogin}.
     */
    ExtractedIdentity scanCard(byte[] photo);

    /** True only if the card's roll number was read confidently and is the resident's own. */
    boolean matchesLogin(ExtractedIdentity scanned, ResidentIdentity resident);
}
