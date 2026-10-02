package co.edu.uptc.universiry.academics.application;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicOfferingDraftCommand(
        UUID periodId,
        UUID curriculumId,
        UUID subjectId,
        String sectionCode,
        LocalDate startsOn,
        LocalDate endsOn,
        int proposedCapacity,
        String sourceReference
) {
}
