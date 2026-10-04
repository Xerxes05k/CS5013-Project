package in.ac.iitm.cs5013.cyclebooking.idverify;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** ID Verification test that needs no native libraries: parsing raw OCR output. */
class IdCardTextParserTest {

    @Test
    void rollNumber_readFromCleanText() {
        assertThat(IdCardTextParser.rollNumber("Roll No: CE24B128\nHostel: Jamuna")).hasValue("CE24B128");
    }

    @Test
    void rollNumber_fixesTypicalOcrConfusionsByPosition() {
        // 0 for O in a letter slot, O for 0 and S for 5 in digit slots.
        assertThat(IdCardTextParser.rollNumber("Roll No: 0E2SBI2O")).hasValue("OE25B120");
        assertThat(IdCardTextParser.rollNumber("roll no: me23b045")).hasValue("ME23B045");
    }

    @Test
    void rollNumber_absentWhenNothingHasTheRightShape() {
        assertThat(IdCardTextParser.rollNumber("Roll No: CE24B12")).isEmpty();
        assertThat(IdCardTextParser.rollNumber("INDIAN INSTITUTE OF TECHNOLOGY")).isEmpty();
    }

    @Test
    void nameAndHostel_readFromLabelledLines() {
        String text = "SYNTHETIC TEST CARD\nName: Test Resident A\nRoll No: CE24B128\nHostel : Jamuna\n";
        assertThat(IdCardTextParser.name(text)).hasValue("Test Resident A");
        assertThat(IdCardTextParser.hostel(text)).hasValue("Jamuna");
    }
}
