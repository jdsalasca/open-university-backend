package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicSiteType;

import java.time.LocalDate;

public record AcademicSiteCommand(
        String code,
        AcademicSiteType type,
        String displayName,
        int displayOrder,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference
) {
}
