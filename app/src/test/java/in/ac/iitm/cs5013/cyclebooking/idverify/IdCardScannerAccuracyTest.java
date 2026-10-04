package in.ac.iitm.cs5013.cyclebooking.idverify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import in.ac.iitm.cs5013.cyclebooking.idverify.SyntheticIdCards.Card;
import in.ac.iitm.cs5013.cyclebooking.idverify.SyntheticIdCards.Condition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Week-2 OCR prototype run: 3 fake cards x 6 photo conditions = 18 scans through the
 * real OpenCV + Tesseract pipeline. Prints an accuracy table for docs/PROGRESS.md.
 *
 * Needs a local Tesseract install (brew install tesseract); skipped, not failed,
 * where there is none.
 */
class IdCardScannerAccuracyTest {

    /** Below this, week 3 will ask the resident to retake the photo. */
    static final float MIN_CONFIDENCE = 70f;

    private static final List<Card> CARDS = List.of(
            new Card("Test Resident A", "CE24B128", "Jamuna"),
            new Card("Test Resident B", "ME23B045", "Jamuna"),
            new Card("Test Resident C", "ED22B019", "Jamuna"));

    private static IdCardScanner scanner;

    @BeforeAll
    static void findTesseract() {
        String tessdata = Stream.of(System.getenv("TESSDATA_PREFIX"),
                        "/opt/homebrew/share/tessdata", "/usr/local/share/tessdata",
                        "/usr/share/tesseract-ocr/5/tessdata")
                .filter(p -> p != null && Files.exists(Path.of(p, "eng.traineddata")))
                .findFirst().orElse(null);
        assumeTrue(tessdata != null, "Tesseract eng.traineddata not installed; skipping OCR prototype");
        scanner = new IdCardScanner(tessdata);
    }

    record Result(Card card, Condition condition, ExtractedIdentity extracted) {
        boolean rollCorrect() {
            return card.rollNumber().equals(extracted.rollNumber());
        }

        boolean confidentlyWrong() {
            return extracted.rollNumber() != null && !rollCorrect() && extracted.confidence() >= MIN_CONFIDENCE;
        }

        boolean accepted() {
            return rollCorrect() && extracted.confidence() >= MIN_CONFIDENCE;
        }
    }

    @Test
    void scanCard_readsRollNumbersAndNeverConfidentlyMisreadsOne() {
        List<Result> results = new ArrayList<>();
        for (Condition condition : Condition.values()) {
            for (Card card : CARDS) {
                results.add(new Result(card, condition, scanner.scanCard(SyntheticIdCards.photo(card, condition))));
            }
        }
        printTable(results);

        // Clean photos must work, or the pipeline is broken.
        assertThat(results).filteredOn(r -> r.condition() == Condition.GOOD).allMatch(Result::accepted);
        // The safety property from the design doc: a bad photo may fail, but must not
        // pass as somebody else's roll number.
        assertThat(results).noneMatch(Result::confidentlyWrong);
    }

    private static void printTable(List<Result> results) {
        System.out.println("\n| Condition | Roll read correctly | Accepted (conf >= " + (int) MIN_CONFIDENCE
                + ") | Mean conf | Confidently wrong |");
        System.out.println("|---|---|---|---|---|");
        for (Condition c : Condition.values()) {
            List<Result> rs = results.stream().filter(r -> r.condition() == c).toList();
            System.out.printf("| %s | %d/%d | %d/%d | %.0f | %d |%n", c,
                    rs.stream().filter(Result::rollCorrect).count(), rs.size(),
                    rs.stream().filter(Result::accepted).count(), rs.size(),
                    rs.stream().mapToDouble(r -> r.extracted().confidence()).average().orElse(0),
                    rs.stream().filter(Result::confidentlyWrong).count());
        }
        for (Result r : results) {
            System.out.printf("  %-11s %s -> %s (conf %.0f) name=%s hostel=%s%n", r.condition(), r.card().rollNumber(),
                    r.extracted().rollNumber(), r.extracted().confidence(), r.extracted().name(), r.extracted().hostel());
        }
    }
}
