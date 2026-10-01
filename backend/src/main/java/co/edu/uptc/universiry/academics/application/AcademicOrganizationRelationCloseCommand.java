package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicStructureRules;

import java.time.LocalDate;

public record AcademicOrganizationRelationCloseCommand(
        LocalDate validFrom,
        LocalDate effectiveThrough,
        String sourceReference
) {
    public AcademicOrganizationRelationCloseCommand {
        if (effectiveThrough == null) {
            throw new IllegalArgumentException("organizationRelation.effectiveThrough is required");
        }
        AcademicStructureRules.validateInterval(validFrom, effectiveThrough, "organizationRelation");
    }
}
