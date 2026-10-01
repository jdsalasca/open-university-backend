package co.edu.uptc.universiry.identity.domain;

public record AssignmentScope(ScopeKind kind, String stableReference) {

    private static final int MAX_REFERENCE_LENGTH = 256;

    public AssignmentScope {
        if (kind == null) {
            throw new IllegalArgumentException("scope kind is required");
        }
        if (kind == ScopeKind.UNIVERSITY) {
            if (stableReference != null) {
                throw new IllegalArgumentException("university scope does not accept a reference");
            }
        } else if (!isValidReference(stableReference)) {
            throw new IllegalArgumentException("scope reference must be present, bounded and free of surrounding whitespace");
        }
    }

    private static boolean isValidReference(String value) {
        return value != null && !value.isBlank() && value.length() <= MAX_REFERENCE_LENGTH
                && value.equals(value.strip()) && value.chars().noneMatch(Character::isISOControl);
    }
}
