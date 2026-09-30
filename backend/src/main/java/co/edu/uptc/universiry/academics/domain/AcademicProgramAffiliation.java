package co.edu.uptc.universiry.academics.domain;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicProgramAffiliation(
        UUID id,
        UUID programId,
        UUID organizationUnitId,
        UUID siteId,
        int displayOrder,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference
) {
    public AcademicProgramAffiliation {
        if (id == null || programId == null || organizationUnitId == null || siteId == null) {
            throw new IllegalArgumentException("programAffiliation.identities are required");
        }
        if (displayOrder < 0 || displayOrder > 100_000) {
            throw new IllegalArgumentException("programAffiliation.displayOrder is invalid");
        }
        AcademicStructureRules.validateInterval(validFrom, validThrough, "programAffiliation");
        sourceReference = sourceReference == null ? "" : sourceReference.trim();
        if (sourceReference.isEmpty() || sourceReference.length() > 240) {
            throw new IllegalArgumentException("programAffiliation.sourceReference is invalid");
        }
    }
}
