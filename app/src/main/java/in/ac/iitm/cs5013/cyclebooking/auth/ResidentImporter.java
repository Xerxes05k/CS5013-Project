package in.ac.iitm.cs5013.cyclebooking.auth;

import in.ac.iitm.cs5013.cyclebooking.persistence.Resident;
import in.ac.iitm.cs5013.cyclebooking.persistence.ResidentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Loads the hostel's residents into the live database (ADR 0006, docs/DEPLOY.md).
 *
 * The prod database starts empty, and a passcode must be stored as a BCrypt hash, so
 * residents can't be added with plain SQL. This reads a CSV of {@code roll_number,name,
 * smail_address}, gives each new resident a random passcode, saves the hash, and writes
 * the plain passcodes to a separate file for the GS to hand out. It runs only when
 * {@code cyclebooking.import.residents} is set on the command line.
 */
@Component
@ConditionalOnProperty("cyclebooking.import.residents")
class ResidentImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ResidentImporter.class);

    /** No 0/O or 1/I/L, so a passcode read off paper can't be misread. */
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int PASSCODE_LENGTH = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ResidentRepository residents;
    private final PasswordEncoder passwordEncoder;
    private final Path residentsCsv;
    private final Path passcodesOut;
    private final String hostel;

    ResidentImporter(ResidentRepository residents, PasswordEncoder passwordEncoder,
            @Value("${cyclebooking.import.residents}") Path residentsCsv,
            @Value("${cyclebooking.import.passcodes-out:passcodes.csv}") Path passcodesOut,
            @Value("${cyclebooking.hostel:Jamuna}") String hostel) {
        this.residents = residents;
        this.passwordEncoder = passwordEncoder;
        this.residentsCsv = residentsCsv;
        this.passcodesOut = passcodesOut;
        this.hostel = hostel;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        int added = importFile(residentsCsv, passcodesOut);
        if (added == 0) {
            log.info("No new residents in {}; nothing saved and no passcodes file written", residentsCsv);
        } else {
            log.info("Imported {} new residents from {}; their passcodes are in {}", added, residentsCsv, passcodesOut);
        }
    }

    /**
     * Adds every resident in {@code csv} not already present (same roll number or smail
     * address) and writes {@code roll_number,name,smail_address,passcode} for each one
     * added to {@code out}. Returns how many were added.
     *
     * All or nothing, so a passcode can never be lost: every line is checked before
     * anything is saved, the passcodes file is written before the residents, and the
     * residents are saved in one transaction. A bad line anywhere, or a failed save,
     * leaves the database and {@code out} as they were. An existing {@code out} is never
     * overwritten, because it may hold passcodes not yet handed out.
     */
    int importFile(Path csv, Path out) throws IOException {
        if (Files.exists(out)) {
            throw new IllegalStateException(out + " already exists. Move it away first; it holds passcodes.");
        }
        List<Resident> toAdd = new ArrayList<>();
        List<String> handOut = new ArrayList<>();
        handOut.add("roll_number,name,smail_address,passcode");
        Set<String> seenInFile = new HashSet<>();

        // 1. Check every line before saving anything.
        List<String> lines = Files.readAllLines(csv);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || (i == 0 && line.toLowerCase().startsWith("roll"))) {
                continue; // blank line or the header row
            }
            String[] f = line.split(",", -1);
            if (f.length != 3 || f[0].isBlank() || f[1].isBlank() || f[2].isBlank()) {
                throw new IllegalArgumentException("Line " + (i + 1) + " of " + csv
                        + ": expected roll_number,name,smail_address (no commas inside a name) but got \"" + line + "\"");
            }
            String roll = f[0].strip().toUpperCase();
            String name = f[1].strip();
            String smail = f[2].strip().toLowerCase();
            if (!seenInFile.add(roll) || !seenInFile.add(smail)) {
                throw new IllegalArgumentException("Line " + (i + 1) + " of " + csv + ": " + roll
                        + " or " + smail + " appears earlier in the file");
            }
            if (residents.findByRollNumberIgnoreCase(roll).isPresent()
                    || residents.findBySmailAddressIgnoreCase(smail).isPresent()) {
                continue; // already loaded: keep their existing passcode
            }
            String passcode = newPasscode();
            toAdd.add(new Resident(roll, name, hostel, smail, passwordEncoder.encode(passcode)));
            handOut.add(roll + "," + name + "," + smail + "," + passcode);
        }
        if (toAdd.isEmpty()) {
            return 0;
        }

        // 2. Passcodes first, readable by this user only, then 3. every resident in one transaction.
        writeOwnerOnly(out, handOut);
        try {
            residents.saveAll(toAdd); // Spring Data runs saveAll in one transaction
        } catch (RuntimeException e) {
            Files.deleteIfExists(out); // nobody was saved, so these passcodes mean nothing
            throw e;
        }
        return toAdd.size();
    }

    /**
     * Creates {@code out} with owner-only permissions (rw-------) on Linux, so other users
     * on the server can't read the plain passcodes. Windows has no POSIX permissions; the
     * file is created plainly there (local testing only, the server runs Linux).
     */
    private static void writeOwnerOnly(Path out, List<String> lines) throws IOException {
        if (out.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.createFile(out, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } else {
            Files.createFile(out);
        }
        Files.write(out, lines, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static String newPasscode() {
        StringBuilder s = new StringBuilder(PASSCODE_LENGTH);
        for (int i = 0; i < PASSCODE_LENGTH; i++) {
            s.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return s.toString();
    }
}
