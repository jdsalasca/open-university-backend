package co.edu.uptc.universiry.academics.application;

import java.time.Instant;
import java.util.UUID;

public record AcademicOfferingDraftAuditEvent(
        long id,
        UUID offeringId,
        AcademicOfferingDraftAuditAction action,
        String actor,
        Instant occurredAt,
        String sourceReference,
        AcademicOfferingDraftSnapshot before,
        AcademicOfferingDraftSnapshot after
) {
}
