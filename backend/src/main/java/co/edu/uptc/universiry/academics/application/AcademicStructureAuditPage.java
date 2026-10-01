package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicStructureAuditEvent;

import java.util.List;

public record AcademicStructureAuditPage(List<AcademicStructureAuditEvent> events, String nextCursor) {
    public AcademicStructureAuditPage {
        events = List.copyOf(events);
    }
}
