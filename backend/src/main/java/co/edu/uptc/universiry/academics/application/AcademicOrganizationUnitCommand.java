package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOrganizationUnitType;

import java.time.LocalDate;

public record AcademicOrganizationUnitCommand(
        String code,
        AcademicOrganizationUnitType type,
        String displayName,
        int displayOrder,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference
) {
}
