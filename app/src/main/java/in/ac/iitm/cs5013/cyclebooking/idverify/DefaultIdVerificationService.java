package in.ac.iitm.cs5013.cyclebooking.idverify;

import in.ac.iitm.cs5013.cyclebooking.auth.ResidentIdentity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Wraps the week-2 OCR pipeline for the booking flow.
 *
 * The scanner is created on first use, not at startup, so the app (and every test
 * that does not scan) still starts on a machine without Tesseract. On such a machine
 * every scan fails, and residents reach the typed-roll fallback after
 * {@link #MAX_FAILED_SCANS} tries.
 */
@Service
class DefaultIdVerificationService implements IdVerificationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultIdVerificationService.class);

    private final String configuredTessdata;
    private IdCardScanner scanner;

    DefaultIdVerificationService(@Value("${cyclebooking.ocr.tessdata:}") String configuredTessdata) {
        this.configuredTessdata = configuredTessdata;
    }

    @Override
    public ExtractedIdentity scanCard(byte[] photo) {
        try {
            return scanOneAtATime(photo);
        } catch (RuntimeException | LinkageError e) {
            // Not an image, OCR crashed, or Tesseract is not installed. To the resident
            // all of these mean "retake the photo"; the log says which it was.
            log.warn("ID card scan failed: {}", e.toString());
            return ExtractedIdentity.unreadable();
        }
    }

    @Override
    public boolean matchesLogin(ExtractedIdentity scanned, ResidentIdentity resident) {
        return scanned.rollNumber() != null
                && scanned.confidence() >= MIN_CONFIDENCE
                && scanned.rollNumber().equalsIgnoreCase(resident.rollNumber());
    }

    // synchronized: one Tesseract instance is not safe to use from two requests at once,
    // and a scan takes about a second, so queueing the odd simultaneous scan is fine.
    private synchronized ExtractedIdentity scanOneAtATime(byte[] photo) {
        if (scanner == null) {
            scanner = new IdCardScanner(findTessdata());
        }
        return scanner.scanCard(photo);
    }

    /** The configured folder if set, otherwise the usual Homebrew and Linux install paths. */
    private String findTessdata() {
        return Stream.of(configuredTessdata, System.getenv("TESSDATA_PREFIX"),
                        "/opt/homebrew/share/tessdata", "/usr/local/share/tessdata",
                        "/usr/share/tesseract-ocr/5/tessdata")
                .filter(p -> p != null && !p.isBlank() && Files.exists(Path.of(p, "eng.traineddata")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tesseract eng.traineddata not found"));
    }
}
