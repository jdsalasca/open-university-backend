package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicPeriodAuditEvent;
import co.edu.uptc.universiry.academics.application.AcademicPeriodHistoryView;

import java.time.Instant;
import java.util.List;

public record AcademicPeriodHistoryResponse(
        AcademicPeriodResponse period,
        List<AcademicCalendarRevisionResponse> calendarRevisions,
        List<AuditEventResponse> auditEvents
) {
    static AcademicPeriodHistoryResponse from(AcademicPeriodHistoryView view) {
        return new AcademicPeriodHistoryResponse(AcademicPeriodResponse.from(view.period()),
                view.calendarRevisions().stream().map(AcademicCalendarRevisionResponse::from).toList(),
                view.auditEvents().stream().map(AuditEventResponse::from).toList());
    }

    public record AuditEventResponse(
            long id,
            String actionKey,
            String actorSub,
            Instant occurredAt,
            String reference,
            String summary
    ) {
        static AuditEventResponse from(AcademicPeriodAuditEvent event) {
            return new AuditEventResponse(event.id(), event.actionKey(), event.actorSub(), event.occurredAt(),
                    event.reference(), event.summary());
        }
    }
}
