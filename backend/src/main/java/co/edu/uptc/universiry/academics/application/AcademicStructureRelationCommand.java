package co.edu.uptc.universiry.academics.application;

import java.time.LocalDate;

public record AcademicStructureRelationCommand(
        int displayOrder,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference
) {
}
