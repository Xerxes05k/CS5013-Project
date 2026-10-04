package in.ac.iitm.cs5013.cyclebooking.idverify;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls the roll number, name and hostel out of raw OCR text.
 *
 * IIT Madras roll numbers look like CE24B128: two letters (department), two digits
 * (year), one letter (programme), three digits. Tesseract regularly confuses O/0,
 * I/1, S/5 and B/8, so each position is read with the confusions that make sense for
 * it, then normalised. A string that still does not fit the shape is not a roll
 * number, which keeps a misread from turning into a confident wrong match.
 */
final class IdCardTextParser {

    // Letter positions also accept the digits OCR mistakes for letters, and vice versa.
    private static final String L = "[A-Z0158]";
    private static final String D = "[0-9OILSB]";
    private static final Pattern ROLL = Pattern.compile(
            "(?<![A-Z0-9])(" + L + L + D + D + L + D + D + D + ")(?![A-Z0-9])");

    private static final Pattern NAME = Pattern.compile("(?im)^\\s*name\\s*[:.;]?\\s*(.+?)\\s*$");
    private static final Pattern HOSTEL = Pattern.compile("(?im)^\\s*hostel\\s*[:.;]?\\s*(.+?)\\s*$");

    private IdCardTextParser() {
    }

    static Optional<String> rollNumber(String text) {
        Matcher m = ROLL.matcher(text.toUpperCase());
        if (!m.find()) {
            return Optional.empty();
        }
        String raw = m.group(1);
        StringBuilder fixed = new StringBuilder(8);
        for (int i = 0; i < raw.length(); i++) {
            boolean letterSlot = i == 0 || i == 1 || i == 4;
            fixed.append(letterSlot ? asLetter(raw.charAt(i)) : asDigit(raw.charAt(i)));
        }
        return Optional.of(fixed.toString());
    }

    static Optional<String> name(String text) {
        return field(NAME, text);
    }

    static Optional<String> hostel(String text) {
        return field(HOSTEL, text);
    }

    private static Optional<String> field(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static char asLetter(char c) {
        return switch (c) {
            case '0' -> 'O';
            case '1' -> 'I';
            case '5' -> 'S';
            case '8' -> 'B';
            default -> c;
        };
    }

    private static char asDigit(char c) {
        return switch (c) {
            case 'O' -> '0';
            case 'I', 'L' -> '1';
            case 'S' -> '5';
            case 'B' -> '8';
            default -> c;
        };
    }
}
