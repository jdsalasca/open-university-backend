package co.edu.uptc.universiry.identity.domain;

import java.net.URI;

public record AuthenticatedPrincipal(String issuer, String subject) {

    private static final int MAX_ISSUER_LENGTH = 2048;
    private static final int MAX_SUBJECT_LENGTH = 255;

    public AuthenticatedPrincipal {
        requireIssuer(issuer);
        requireSubject(subject);
    }

    private static void requireIssuer(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_ISSUER_LENGTH
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("identity issuer must be present, bounded and free of control characters");
        }
        try {
            URI issuerUri = URI.create(value);
            if (!"https".equalsIgnoreCase(issuerUri.getScheme()) || issuerUri.getHost() == null
                    || issuerUri.getRawQuery() != null || issuerUri.getRawFragment() != null
                    || issuerUri.getRawUserInfo() != null) {
                throw new IllegalArgumentException("identity issuer must be an HTTPS URL without query or fragment");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("identity issuer must be a valid HTTPS URL", exception);
        }
    }

    private static void requireSubject(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_SUBJECT_LENGTH
                || value.chars().anyMatch(Character::isISOControl)
                || value.chars().anyMatch(character -> character > 0x7f)) {
            throw new IllegalArgumentException("identity subject must be present, case-sensitive ASCII and at most 255 characters");
        }
    }
}
