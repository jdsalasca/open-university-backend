package co.edu.uptc.universiry.academics.application;

import java.time.LocalDate;

public record AcademicOfferingDraftUpdateCommand(
        int expectedVersion,
        String sectionCode,
        LocalDate startsOn,
        LocalDate endsOn,
        int proposedCapacity,
        String sourceReference
) {
}
