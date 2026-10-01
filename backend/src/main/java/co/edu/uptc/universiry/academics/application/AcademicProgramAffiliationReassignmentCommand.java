package co.edu.uptc.universiry.academics.application;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicProgramAffiliationReassignmentCommand(
        LocalDate expectedValidFrom,
        LocalDate expectedValidThrough,
        LocalDate effectiveFrom,
        UUID organizationUnitId,
        UUID siteId,
        int displayOrder,
        String sourceReference
) {
}
