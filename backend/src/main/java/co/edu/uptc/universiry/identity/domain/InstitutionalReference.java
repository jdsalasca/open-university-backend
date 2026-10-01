package co.edu.uptc.universiry.identity.domain;

public record InstitutionalReference(String value) {

    private static final int MAX_LENGTH = 512;

    public InstitutionalReference {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH
                || !value.equals(value.strip()) || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("institutional reference must be present, bounded and free of control characters");
        }
    }
}
