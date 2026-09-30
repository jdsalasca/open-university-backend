package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCalendarRevision;

import java.util.List;

public record AcademicPeriodHistoryView(
        AcademicPeriodView period,
        List<AcademicCalendarRevision> calendarRevisions,
        List<AcademicPeriodAuditEvent> auditEvents
) {
    public AcademicPeriodHistoryView {
        calendarRevisions = List.copyOf(calendarRevisions);
        auditEvents = List.copyOf(auditEvents);
    }
}
