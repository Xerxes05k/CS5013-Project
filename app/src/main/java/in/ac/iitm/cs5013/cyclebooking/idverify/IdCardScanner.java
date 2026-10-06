package in.ac.iitm.cs5013.cyclebooking.idverify;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;

/**
 * The ID Verification pipeline: photo bytes in, extracted text out. Used by
 * DefaultIdVerificationService at booking time. It never writes the photo anywhere;
 * the bytes only live for the duration of the call (ADR 0003).
 */
public class IdCardScanner {

    private final Tesseract tesseract;

    /** @param tessdataPath folder holding eng.traineddata, e.g. /opt/homebrew/share/tessdata */
    public IdCardScanner(String tessdataPath) {
        tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("eng");
        // Page segmentation mode 6: treat the card as one uniform block of text.
        tesseract.setPageSegMode(6);
    }

    /**
     * Reads the three versions of the photo from {@link IdCardPreprocessor#variants}
     * and returns the roll number most of them agree on; between equally common
     * readings, the more confident one. Each version gets one vote, so a single version
     * misreading a digit is outvoted by the two that read it right.
     */
    public ExtractedIdentity scanCard(byte[] photo) {
        List<ExtractedIdentity> reads = new ArrayList<>();
        for (byte[] version : IdCardPreprocessor.variants(photo)) {
            reads.add(read(toImage(version)));
        }
        return vote(reads);
    }

    static ExtractedIdentity vote(List<ExtractedIdentity> reads) {
        Map<String, Long> votes = reads.stream()
                .filter(r -> r.rollNumber() != null)
                .collect(Collectors.groupingBy(ExtractedIdentity::rollNumber, Collectors.counting()));
        return reads.stream()
                .filter(r -> r.rollNumber() != null)
                .max(Comparator.<ExtractedIdentity>comparingLong(r -> votes.get(r.rollNumber()))
                        .thenComparingDouble(ExtractedIdentity::confidence))
                .orElse(ExtractedIdentity.unreadable());
    }

    private ExtractedIdentity read(BufferedImage card) {
        try {
            String text = tesseract.doOCR(card);
            String roll = IdCardTextParser.rollNumber(text).orElse(null);
            float confidence = roll == null ? 0f : rollConfidence(
                    tesseract.getWords(card, TessPageIteratorLevel.RIL_WORD),
                    tesseract.getWords(card, TessPageIteratorLevel.RIL_SYMBOL));
            return new ExtractedIdentity(
                    IdCardTextParser.name(text).orElse(null),
                    roll,
                    IdCardTextParser.hostel(text).orElse(null),
                    confidence);
        } catch (TesseractException e) {
            throw new IllegalStateException("OCR failed", e);
        }
    }

    /**
     * How sure OCR is of the roll number: the confidence of its weakest letter or digit.
     *
     * Not Tesseract's score for the whole word. On a real IIT Madras card that came out
     * at 42 while every one of the 8 characters scored 98-99: a speck beside the number
     * was read as a full stop, and whole-word scores run low for the card's bold serif
     * font. We act on the 8 characters, so they are what gets scored, and one badly
     * read character pulls the score down. It is not proof against misreads, though:
     * on real cards OCR sometimes reads an 8 as a 5 at 95+. What stops a misread from
     * booking is that the roll number must equal the logged-in resident's own.
     */
    static float rollConfidence(List<Word> words, List<Word> symbols) {
        double best = 0;
        for (Word word : words) {
            if (IdCardTextParser.rollNumber(word.getText()).isEmpty()) {
                continue;
            }
            double weakest = symbols.stream()
                    .filter(c -> c.getText().length() == 1 && Character.isLetterOrDigit(c.getText().charAt(0)))
                    .filter(c -> word.getBoundingBox().contains(centre(c.getBoundingBox())))
                    .mapToDouble(Word::getConfidence)
                    .min()
                    .orElse(word.getConfidence());
            best = Math.max(best, weakest);
        }
        return (float) best;
    }

    private static Point centre(Rectangle r) {
        return new Point((int) r.getCenterX(), (int) r.getCenterY());
    }

    private static BufferedImage toImage(byte[] png) {
        try {
            return ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
