package in.ac.iitm.cs5013.cyclebooking.idverify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * ID Verification test, no native libraries: how the three OCR readings of one photo
 * are combined. The 8-read-as-5 misread is the one seen on a real card.
 */
class IdCardScannerVoteTest {

    private static ExtractedIdentity read(String roll, float confidence) {
        return new ExtractedIdentity(null, roll, null, confidence);
    }

    @Test
    void vote_majorityOutvotesAMoreConfidentMisread() {
        ExtractedIdentity result = IdCardScanner.vote(List.of(
                read("CE24B128", 91), read("CE24B125", 98), read("CE24B128", 94)));

        assertThat(result.rollNumber()).isEqualTo("CE24B128");
        assertThat(result.confidence()).isEqualTo(94f);
    }

    @Test
    void vote_tieGoesToTheMoreConfidentReading() {
        ExtractedIdentity result = IdCardScanner.vote(List.of(
                ExtractedIdentity.unreadable(), read("CE24B125", 95), read("CE24B128", 97)));

        assertThat(result.rollNumber()).isEqualTo("CE24B128");
    }

    @Test
    void vote_oneReadingIsEnoughWhenTheOthersFoundNothing() {
        ExtractedIdentity result = IdCardScanner.vote(List.of(
                ExtractedIdentity.unreadable(), read("CE24B128", 98), ExtractedIdentity.unreadable()));

        assertThat(result.rollNumber()).isEqualTo("CE24B128");
    }

    @Test
    void vote_isUnreadableWhenNoVersionFoundARollNumber() {
        assertThat(IdCardScanner.vote(List.of(ExtractedIdentity.unreadable(), ExtractedIdentity.unreadable())))
                .isEqualTo(ExtractedIdentity.unreadable());
    }
}
