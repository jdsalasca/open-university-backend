package co.edu.uptc.universiry.academics.domain;

import java.time.Instant;
import java.util.UUID;

public record AcademicStructureAuditEvent(
        long id,
        UUID entityId,
        AcademicStructureAuditAction actionKey,
        String actor,
        String reference,
        Instant occurredAt,
        String summary
) {
}
