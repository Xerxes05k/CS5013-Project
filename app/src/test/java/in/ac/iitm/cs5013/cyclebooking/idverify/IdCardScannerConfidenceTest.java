package in.ac.iitm.cs5013.cyclebooking.idverify;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.tess4j.Word;
import org.junit.jupiter.api.Test;

/**
 * ID Verification test, no native libraries: the roll number is scored by its weakest
 * character, not by Tesseract's whole-word score. The numbers mirror a real card scan
 * where the word scored 42 but every character 98-99.
 */
class IdCardScannerConfidenceTest {

    private static final Rectangle ROLL_BOX = new Rectangle(380, 495, 340, 50);

    /** One OCR symbol per character, laid out left to right inside the roll word's box. */
    private static List<Word> symbols(String text, float... conf) {
        List<Word> out = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            out.add(new Word(String.valueOf(text.charAt(i)), conf[i], new Rectangle(385 + i * 40, 500, 35, 40)));
        }
        return out;
    }

    @Test
    void rollConfidence_ignoresStrayPunctuationThatSinksTheWordScore() {
        List<Word> words = List.of(new Word("CE24B128.", 41.8f, ROLL_BOX));
        List<Word> chars = symbols("CE24B128.", 99, 98, 99, 99, 98, 99, 99, 99, 12);

        assertThat(IdCardScanner.rollConfidence(words, chars)).isEqualTo(98f);
    }

    @Test
    void rollConfidence_isPulledDownByOneBadlyReadCharacter() {
        List<Word> words = List.of(new Word("CE24B128", 80f, ROLL_BOX));
        List<Word> chars = symbols("CE24B128", 99, 99, 99, 35, 99, 99, 99, 99);

        assertThat(IdCardScanner.rollConfidence(words, chars)).isEqualTo(35f);
    }

    @Test
    void rollConfidence_ignoresCharactersOutsideTheRollWord() {
        List<Word> words = List.of(new Word("CE24B128", 80f, ROLL_BOX),
                new Word("KATYAL", 20f, new Rectangle(380, 400, 300, 50)));
        List<Word> chars = new ArrayList<>(symbols("CE24B128", 97, 99, 99, 99, 99, 99, 99, 99));
        chars.add(new Word("K", 5f, new Rectangle(385, 405, 35, 40)));

        assertThat(IdCardScanner.rollConfidence(words, chars)).isEqualTo(97f);
    }

    @Test
    void rollConfidence_isZeroWhenNoWordLooksLikeARollNumber() {
        assertThat(IdCardScanner.rollConfidence(List.of(new Word("B.TECH", 90f, ROLL_BOX)), List.of())).isZero();
    }
}
