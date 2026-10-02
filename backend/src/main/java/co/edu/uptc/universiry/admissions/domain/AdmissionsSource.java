package co.edu.uptc.universiry.admissions.domain;

import java.net.URI;

public record AdmissionsSource(String label, String url) {

    public AdmissionsSource {
        label = requiredText(label, 120, "source.label");
        url = requireHttpsUrl(url, "source.url");
    }

    static String requiredText(String value, int maximumLength, String field) {
        if (value == null || value.isBlank() || value.trim().length() > maximumLength) {
            throw new IllegalArgumentException(field + " is required and must fit its maximum length");
        }
        String normalized = value.trim();
        if (normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + " must not contain control characters");
        }
        return normalized;
    }

    static String requireHttpsUrl(String value, String field) {
        if (value == null || value.isBlank() || value.trim().length() > 500) {
            throw new IllegalArgumentException(field + " is required and must fit its maximum length");
        }
        String normalized = value.trim();
        try {
            URI uri = URI.create(normalized);
            if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getPort() > 443
                    || normalized.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException(field + " must be a public HTTPS URL");
            }
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(field + " must be a public HTTPS URL", invalid);
        }
        return normalized;
    }
}
