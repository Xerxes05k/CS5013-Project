package in.ac.iitm.cs5013.cyclebooking.idverify;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;

/**
 * Week-2 prototype of the ID Verification pipeline: photo bytes in, extracted text
 * out. Not wired into booking yet (week 3), and it never writes the photo anywhere;
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

    public ExtractedIdentity scanCard(byte[] photo) {
        BufferedImage cleaned = toImage(IdCardPreprocessor.clean(photo));
        try {
            String text = tesseract.doOCR(cleaned);
            List<Word> words = tesseract.getWords(cleaned, TessPageIteratorLevel.RIL_WORD);

            String roll = IdCardTextParser.rollNumber(text).orElse(null);
            float confidence = roll == null ? 0f : rollWordConfidence(words);
            return new ExtractedIdentity(
                    IdCardTextParser.name(text).orElse(null),
                    roll,
                    IdCardTextParser.hostel(text).orElse(null),
                    confidence);
        } catch (TesseractException e) {
            throw new IllegalStateException("OCR failed", e);
        }
    }

    /** Confidence of the word that looks like the roll number, i.e. the one we act on. */
    private static float rollWordConfidence(List<Word> words) {
        return (float) words.stream()
                .filter(w -> IdCardTextParser.rollNumber(w.getText()).isPresent())
                .mapToDouble(Word::getConfidence)
                .max()
                .orElse(0);
    }

    private static BufferedImage toImage(byte[] png) {
        try {
            return ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
