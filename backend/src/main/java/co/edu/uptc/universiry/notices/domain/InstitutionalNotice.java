package co.edu.uptc.universiry.notices.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * A published institutional notice. Publication is a complete, immutable act: a notice is never edited, so a
 * correction is a new notice and the historical audience of the previous one stays auditable.
 */
public record InstitutionalNotice(
        String noticeId,
        String title,
        String body,
        String sourceReference,
        LocalDate publishedFrom,
        LocalDate publishedThrough,
        List<NoticeAudience> audiences,
        String publishedBy,
        Instant publishedAt
) {

    public static final int MAX_AUDIENCES = 50;

    public InstitutionalNotice {
        title = requiredText(title, 160, "title");
        body = requiredText(body, 2000, "body");
        sourceReference = requiredText(sourceReference, 240, "sourceReference");
        publishedBy = requiredText(publishedBy, 255, "publishedBy");
        if (publishedFrom == null || publishedThrough == null || publishedThrough.isBefore(publishedFrom)) {
            throw new IllegalArgumentException("a notice requires an availability window that is not inverted");
        }
        List<NoticeAudience> copied = audiences == null ? List.of() : List.copyOf(audiences);
        if (copied.isEmpty() || copied.size() > MAX_AUDIENCES) {
            throw new IllegalArgumentException("a notice requires between 1 and 50 audiences");
        }
        if (new HashSet<>(copied).size() != copied.size()) {
            throw new IllegalArgumentException("a notice cannot repeat the same audience twice");
        }
        // Ordered from the broadest audience to the narrowest, so every surface renders the same reading order.
        audiences = copied.stream()
                .sorted(Comparator.comparingInt((NoticeAudience audience) -> audience.kind().ordinal())
                        .thenComparing(audience -> audience.reference() == null ? "" : audience.reference()))
                .toList();
    }

    private static String requiredText(String value, int maximumLength, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maximumLength) {
            throw new IllegalArgumentException(field + " cannot exceed " + maximumLength + " characters");
        }
        return trimmed;
    }
}