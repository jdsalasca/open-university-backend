package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicStructureAuditPage;
import co.edu.uptc.universiry.academics.domain.AcademicStructureAuditAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AcademicStructureAuditPageResponse(List<AuditEventResponse> events, String nextCursor) {
    public AcademicStructureAuditPageResponse {
        events = List.copyOf(events);
    }

    static AcademicStructureAuditPageResponse from(AcademicStructureAuditPage page) {
        return new AcademicStructureAuditPageResponse(page.events().stream().map(event -> new AuditEventResponse(
                event.entityId(), event.actionKey(), event.actor(), event.occurredAt(), event.reference(),
                event.summary())).toList(), page.nextCursor());
    }

    public record AuditEventResponse(
            UUID entityId,
            AcademicStructureAuditAction actionKey,
            String actor,
            Instant occurredAt,
            String reference,
            String summary
    ) {
    }
}
