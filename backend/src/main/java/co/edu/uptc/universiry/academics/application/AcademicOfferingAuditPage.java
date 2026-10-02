package co.edu.uptc.universiry.academics.application;

import java.util.List;

public record AcademicOfferingAuditPage(List<AcademicOfferingDraftAuditEvent> events, String nextCursor) {
    public AcademicOfferingAuditPage {
        events = List.copyOf(events);
    }
}
