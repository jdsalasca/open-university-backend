package co.edu.uptc.universiry.identity.domain;

public record AuthenticatedPrincipal(String issuer, String subject) {

    private static final int MAX_OPAQUE_VALUE_LENGTH = 2048;

    public AuthenticatedPrincipal {
        requireOpaqueValue(issuer, "issuer");
        requireOpaqueValue(subject, "subject");
    }

    private static void requireOpaqueValue(String value, String name) {
        if (value == null || value.isBlank() || value.length() > MAX_OPAQUE_VALUE_LENGTH
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("identity " + name + " must be present, bounded and free of control characters");
        }
    }
}
