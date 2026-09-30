package co.edu.uptc.universiry.academics.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AcademicCatalogValueRules {

    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$");
    private static final Pattern COHORT_TERM = Pattern.compile("^(\\d{4})-([12])$");

    private AcademicCatalogValueRules() {
    }

    static UUID requiredId(UUID value, String field) {
        if (value == null) {
            throw invalid(field, "is required");
        }
        return value;
    }

    static String identifier(String value, int maxLength, String field) {
        String trimmed = requiredText(value, maxLength, field);
        if (!IDENTIFIER.matcher(trimmed).matches()) {
            throw invalid(field, "must use letters, numbers, dot, underscore or hyphen");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    static String requiredText(String value, int maxLength, String field) {
        if (value == null) {
            throw invalid(field, "is required");
        }

        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw invalid(field, "is required");
        }
        if (length(trimmed) > maxLength) {
            throw invalid(field, "exceeds its maximum length");
        }
        return trimmed;
    }

    static String optionalText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.strip();
        if (length(trimmed) > maxLength) {
            throw invalid(field, "exceeds its maximum length");
        }
        return trimmed;
    }

    static BigDecimal credits(BigDecimal value) {
        if (value == null || value.signum() <= 0
                || value.compareTo(AcademicCatalogLimits.MAX_CREDITS) > 0
                || value.scale() > AcademicCatalogLimits.MAX_CREDIT_SCALE) {
            throw invalid("credits", "must be positive, at most 999.99 and use no more than two decimals");
        }

        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }

    static CohortTerm cohortTerm(String value, String field) {
        String normalized = requiredText(value, AcademicCatalogLimits.MAX_COHORT_TERM_LENGTH, field);
        Matcher matcher = COHORT_TERM.matcher(normalized);
        if (!matcher.matches()) {
            throw invalid(field, "must use YYYY-1 or YYYY-2");
        }
        return new CohortTerm(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    static IllegalArgumentException invalid(String field, String reason) {
        return new IllegalArgumentException(field + " " + reason + ".");
    }

    private static int length(String value) {
        return value.codePointCount(0, value.length());
    }

    record CohortTerm(int year, int term) implements Comparable<CohortTerm> {
        @Override
        public int compareTo(CohortTerm other) {
            int yearComparison = Integer.compare(year, other.year);
            return yearComparison != 0 ? yearComparison : Integer.compare(term, other.term);
        }
    }
}
