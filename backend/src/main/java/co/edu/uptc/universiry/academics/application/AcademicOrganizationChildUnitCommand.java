package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnitType;

import java.time.LocalDate;

public record AcademicOrganizationChildUnitCommand(
        String code,
        AcademicOrganizationUnitType type,
        String displayName,
        int relationshipDisplayOrder,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference
) {
}
