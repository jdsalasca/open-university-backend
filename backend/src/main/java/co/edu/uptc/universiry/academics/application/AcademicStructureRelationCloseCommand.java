package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicStructureRules;

import java.time.LocalDate;

public record AcademicStructureRelationCloseCommand(
        LocalDate validFrom,
        LocalDate effectiveThrough,
        String sourceReference
) {
    public AcademicStructureRelationCloseCommand {
        if (effectiveThrough == null) {
            throw new IllegalArgumentException("structureRelation.effectiveThrough is required");
        }
        AcademicStructureRules.validateInterval(validFrom, effectiveThrough, "structureRelation");
    }
}
