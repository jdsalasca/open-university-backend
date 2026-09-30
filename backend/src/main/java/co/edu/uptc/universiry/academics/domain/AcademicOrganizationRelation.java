package co.edu.uptc.universiry.academics.domain;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicOrganizationRelation(
        UUID parentUnitId,
        UUID childUnitId,
        int displayOrder,
        LocalDate validFrom,
        LocalDate validThrough
) {
    public AcademicOrganizationRelation {
        if (parentUnitId == null || childUnitId == null || parentUnitId.equals(childUnitId)) {
            throw new IllegalArgumentException("organizationRelation.endpoints are invalid");
        }
        if (displayOrder < 0) throw new IllegalArgumentException("organizationRelation.displayOrder is invalid");
        AcademicStructureRules.validateInterval(validFrom, validThrough, "organizationRelation");
    }
}
