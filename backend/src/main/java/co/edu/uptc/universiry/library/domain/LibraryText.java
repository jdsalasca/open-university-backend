package co.edu.uptc.universiry.library.domain;

/**
 * Shared text validation for the library domain, so every bibliographic and loan record applies the same bounded,
 * printable text rule instead of repeating it.
 */
final class LibraryText {

    private LibraryText() {
    }

    static String required(String value, int maximumLength, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maximumLength || containsControlCharacter(trimmed)) {
            throw new IllegalArgumentException(field + " is not a bounded printable text");
        }
        return trimmed;
    }

    private static boolean containsControlCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }
}