package co.edu.uptc.universiry.spaces.domain;

import java.net.URI;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public record SpaceSource(String label, String url, LocalDate checkedAt, LocalDate sourceUpdatedAt) {

    public SpaceSource {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Source label must not be blank.");
        }
        if (!isHttpsUrl(url)) {
            throw new IllegalArgumentException("Source URL must use HTTPS.");
        }
        Objects.requireNonNull(checkedAt, "Source checkedAt must not be null.");
        if (sourceUpdatedAt != null && sourceUpdatedAt.isAfter(checkedAt)) {
            throw new IllegalArgumentException("Source update date cannot be after checkedAt.");
        }
    }

    static boolean isHttpsUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute() && "https".equals(uri.getScheme().toLowerCase(Locale.ROOT))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
