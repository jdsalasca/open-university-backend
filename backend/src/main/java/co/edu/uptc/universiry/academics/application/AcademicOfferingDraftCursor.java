package co.edu.uptc.universiry.academics.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record AcademicOfferingDraftCursor(Instant createdAt, UUID id) {

    public AcademicOfferingDraftCursor {
        if (createdAt == null || id == null) throw invalid();
    }

    public static AcademicOfferingDraftCursor parse(String token) {
        if (token == null) return null;
        if (token.isBlank() || token.length() > 160) throw invalid();
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) throw invalid();
            var cursor = new AcademicOfferingDraftCursor(Instant.parse(parts[1]), UUID.fromString(parts[2]));
            if (!cursor.encode().equals(token)) throw invalid();
            return cursor;
        } catch (RuntimeException exception) {
            throw invalid();
        }
    }

    public String encode() {
        String payload = "v1|" + createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("The academic offering cursor is invalid.");
    }
}
