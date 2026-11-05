package in.ac.iitm.cs5013.cyclebooking.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Loading residents for the live site: hashed passcodes saved, plain ones written once
 * for the GS, and all or nothing so a passcode can never be lost.
 */
class ResidentImporterTest {

    @TempDir
    Path dir;

    private final ResidentRepository residents = mock(ResidentRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private ResidentImporter importer;

    @BeforeEach
    void setUp() {
        when(residents.findByRollNumberIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(residents.findBySmailAddressIgnoreCase(anyString())).thenReturn(Optional.empty());
        importer = new ResidentImporter(residents, encoder, dir.resolve("in.csv"), dir.resolve("out.csv"), "Jamuna");
    }

    private Path csv(String... lines) throws Exception {
        return Files.write(dir.resolve("residents.csv"), List.of(lines));
    }

    @SuppressWarnings("unchecked")
    private List<Resident> savedResidents() {
        ArgumentCaptor<List<Resident>> saved = ArgumentCaptor.forClass(List.class);
        verify(residents).saveAll(saved.capture());
        return saved.getValue();
    }

    @Test
    void newResidentGetsAHashedPasscodeAndThePlainOneIsWrittenForTheGs() throws Exception {
        Path out = dir.resolve("passcodes.csv");

        int added = importer.importFile(csv("roll_number,name,smail_address", "ce24b001, Aditya Gautam ,CE24B001@smail.iitm.ac.in"), out);

        Resident saved = savedResidents().get(0);
        assertThat(added).isEqualTo(1);
        assertThat(saved.getRollNumber()).isEqualTo("CE24B001");
        assertThat(saved.getSmailAddress()).isEqualTo("ce24b001@smail.iitm.ac.in");
        assertThat(saved.getHostel()).isEqualTo("Jamuna");

        List<String> written = Files.readAllLines(out);
        assertThat(written).hasSize(2);
        String passcode = written.get(1).substring(written.get(1).lastIndexOf(',') + 1);
        assertThat(passcode).hasSize(ResidentImporter.PASSCODE_LENGTH);
        assertThat(encoder.matches(passcode, saved.getPasscodeHash())).isTrue();
    }

    /** Yashas's reproduction from the #16 review: a comma inside a name, after a good line. */
    @Test
    void badLineAfterAGoodOneSavesNobody() throws Exception {
        Path in = csv("roll_number,name,smail_address",
                "CE24B001,Aditya Gautam,ce24b001@smail.iitm.ac.in",
                "CE24B002,Katyal, Yashas,ce24b002@smail.iitm.ac.in");
        Path out = dir.resolve("p.csv");

        assertThatThrownBy(() -> importer.importFile(in, out)).isInstanceOf(IllegalArgumentException.class);
        verify(residents, never()).save(any());
        verify(residents, never()).saveAll(anyIterable());
        assertThat(out).doesNotExist();
    }

    @Test
    void sameResidentTwiceInTheFileIsRefusedBeforeAnythingIsSaved() throws Exception {
        Path in = csv("CE24B001,Aditya Gautam,ce24b001@smail.iitm.ac.in",
                "CE24B001,Aditya G,other@smail.iitm.ac.in");

        assertThatThrownBy(() -> importer.importFile(in, dir.resolve("p.csv")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line 2");
        verify(residents, never()).saveAll(anyIterable());
    }

    @Test
    void failedSaveRemovesThePasscodesFileSoNothingIsHalfDone() throws Exception {
        when(residents.saveAll(anyIterable())).thenThrow(new DataIntegrityViolationException("unique roll_number"));
        Path out = dir.resolve("p.csv");

        assertThatThrownBy(() -> importer.importFile(csv("CE24B001,Aditya Gautam,ce24b001@smail.iitm.ac.in"), out))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(out).doesNotExist();
    }

    @Test
    void residentAlreadyLoadedIsSkippedSoTheirPasscodeStays() throws Exception {
        when(residents.findByRollNumberIgnoreCase("CE24B128")).thenReturn(Optional.of(
                new Resident("CE24B128", "Yashas Katyal", "Jamuna", "ce24b128@smail.iitm.ac.in", "hash")));
        Path out = dir.resolve("p.csv");

        int added = importer.importFile(csv("CE24B128,Yashas Katyal,ce24b128@smail.iitm.ac.in"), out);

        assertThat(added).isZero();
        verify(residents, never()).saveAll(anyIterable());
        assertThat(out).doesNotExist();
    }

    @Test
    void existingPasscodeFileIsNeverOverwritten() throws Exception {
        Path out = Files.writeString(dir.resolve("p.csv"), "passcodes already handed out");

        assertThatThrownBy(() -> importer.importFile(csv("CE24B001,Aditya Gautam,ce24b001@smail.iitm.ac.in"), out))
                .isInstanceOf(IllegalStateException.class);
        assertThat(Files.readString(out)).isEqualTo("passcodes already handed out");
        verify(residents, never()).saveAll(anyIterable());
    }

    @Test
    void malformedLineIsReportedWithItsLineNumber() throws Exception {
        assertThatThrownBy(() -> importer.importFile(csv("roll_number,name,smail_address", "CE24B001,Aditya Gautam"),
                dir.resolve("p.csv")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line 2");
    }

    /** Runs on Linux (the server); skipped on Windows, which has no POSIX permissions. */
    @Test
    void passcodesFileIsReadableByItsOwnerOnly() throws Exception {
        assumeTrue(dir.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Path out = dir.resolve("p.csv");

        importer.importFile(csv("CE24B001,Aditya Gautam,ce24b001@smail.iitm.ac.in"), out);

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(out))).isEqualTo("rw-------");
    }

    @Test
    void passcodesAvoidCharactersThatAreEasyToMisread() {
        for (int i = 0; i < 200; i++) {
            assertThat(ResidentImporter.newPasscode()).doesNotContainPattern("[0O1IL]");
        }
    }
}
