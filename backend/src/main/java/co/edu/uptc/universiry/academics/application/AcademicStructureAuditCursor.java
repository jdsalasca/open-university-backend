package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicStructureAuditEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record AcademicStructureAuditCursor(Instant occurredAt, long eventId) {

    public AcademicStructureAuditCursor {
        if (occurredAt == null || eventId < 1) {
            throw new IllegalArgumentException("The audit cursor is invalid.");
        }
    }

    public static AcademicStructureAuditCursor parse(String token) {
        if (token == null) return null;
        if (token.isBlank() || token.length() > 128) throw invalid();
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) throw invalid();
            var cursor = new AcademicStructureAuditCursor(Instant.parse(parts[1]), Long.parseLong(parts[2]));
            if (!cursor.encode().equals(token)) throw invalid();
            return cursor;
        } catch (RuntimeException exception) {
            throw invalid();
        }
    }

    public static AcademicStructureAuditCursor from(AcademicStructureAuditEvent event) {
        return new AcademicStructureAuditCursor(event.occurredAt(), event.id());
    }

    public String encode() {
        String payload = "v1|" + occurredAt + "|" + eventId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("The audit cursor is invalid.");
    }
}
