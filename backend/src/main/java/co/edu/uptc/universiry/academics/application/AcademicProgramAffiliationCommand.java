package co.edu.uptc.universiry.academics.application;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicProgramAffiliationCommand(
        UUID organizationUnitId,
        UUID siteId,
        int displayOrder,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference
) {
}
