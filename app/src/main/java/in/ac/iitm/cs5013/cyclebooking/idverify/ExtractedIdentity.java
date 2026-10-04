package in.ac.iitm.cs5013.cyclebooking.idverify;

/**
 * What OCR read off an ID card. Any field may be null if it could not be read.
 * Confidence is Tesseract's 0-100 score for the word that gave the roll number;
 * 0 when no roll number was found.
 */
public record ExtractedIdentity(String name, String rollNumber, String hostel, float confidence) {

    /** The result for a photo nothing could be read from. */
    public static ExtractedIdentity unreadable() {
        return new ExtractedIdentity(null, null, null, 0f);
    }
}
