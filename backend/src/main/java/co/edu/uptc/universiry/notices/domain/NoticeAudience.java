package co.edu.uptc.universiry.notices.domain;

/**
 * One audience of an institutional notice. The university scope is the whole community and therefore carries no
 * reference; every narrower scope points to an opaque stable identity of the academic structure.
 */
public record NoticeAudience(NoticeAudienceKind kind, String reference) {

    private static final int MAX_REFERENCE_LENGTH = 64;

    public NoticeAudience {
        if (kind == null) {
            throw new IllegalArgumentException("an audience requires a kind");
        }
        if (kind == NoticeAudienceKind.UNIVERSITY) {
            if (reference != null && !reference.isBlank()) {
                throw new IllegalArgumentException("a university-wide audience cannot carry a scope reference");
            }
            reference = null;
        } else {
            if (reference == null || reference.isBlank()) {
                throw new IllegalArgumentException("a scoped audience requires a stable reference");
            }
            String trimmed = reference.trim();
            if (trimmed.length() > MAX_REFERENCE_LENGTH) {
                throw new IllegalArgumentException("an audience reference cannot exceed 64 characters");
            }
            reference = trimmed;
        }
    }
}