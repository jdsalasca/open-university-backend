package co.edu.uptc.universiry.academics.application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record AcademicOfferingAuditCursor(long eventId) {
    public AcademicOfferingAuditCursor {
        if (eventId < 1) throw invalid();
    }

    public static AcademicOfferingAuditCursor parse(String token) {
        if (token == null) return null;
        if (token.isBlank() || token.length() > 80) throw invalid();
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 2 || !"v1".equals(parts[0])) throw invalid();
            var cursor = new AcademicOfferingAuditCursor(Long.parseLong(parts[1]));
            if (!cursor.encode().equals(token)) throw invalid();
            return cursor;
        } catch (RuntimeException exception) {
            throw invalid();
        }
    }

    public String encode() {
        String payload = "v1|" + eventId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("The academic offering audit cursor is invalid.");
    }
}
