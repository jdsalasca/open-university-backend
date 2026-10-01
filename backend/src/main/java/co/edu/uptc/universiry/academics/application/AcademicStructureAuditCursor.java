package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicStructureAuditEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public record AcademicStructureAuditCursor(Instant occurredAt, long eventId) {

    private static final Instant MIN_MYSQL_TIMESTAMP = Instant.parse("1970-01-01T00:00:01Z");
    private static final Instant MAX_MYSQL_TIMESTAMP = Instant.parse("2038-01-19T03:14:07.499999Z");

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
            Instant occurredAt = Instant.parse(parts[1]);
            if (occurredAt.isBefore(MIN_MYSQL_TIMESTAMP) || occurredAt.isAfter(MAX_MYSQL_TIMESTAMP)) {
                throw invalid();
            }
            var cursor = new AcademicStructureAuditCursor(occurredAt, Long.parseLong(parts[2]));
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
