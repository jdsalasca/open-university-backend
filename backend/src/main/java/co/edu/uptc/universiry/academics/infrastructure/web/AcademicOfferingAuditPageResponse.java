package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicOfferingAuditPage;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftAuditEvent;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AcademicOfferingAuditPageResponse(List<AuditEventResponse> events, String nextCursor) {
    public AcademicOfferingAuditPageResponse {
        events = List.copyOf(events);
    }

    static AcademicOfferingAuditPageResponse from(AcademicOfferingAuditPage page) {
        return new AcademicOfferingAuditPageResponse(page.events().stream().map(AuditEventResponse::from).toList(),
                page.nextCursor());
    }

    public record AuditEventResponse(
            long id,
            UUID offeringId,
            String actionKey,
            String actor,
            Instant occurredAt,
            String sourceReference,
            AcademicOfferingDraftSnapshot before,
            AcademicOfferingDraftSnapshot after
    ) {
        static AuditEventResponse from(AcademicOfferingDraftAuditEvent event) {
            return new AuditEventResponse(event.id(), event.offeringId(), event.action().name(), event.actor(),
                    event.occurredAt(), event.sourceReference(), event.before(), event.after());
        }
    }
}
