package co.edu.uptc.universiry.academics.application;

import java.time.Instant;

public record AcademicPeriodAuditEvent(
        long id,
        String actionKey,
        String actorSub,
        Instant occurredAt,
        String reference,
        String summary
) {
}
