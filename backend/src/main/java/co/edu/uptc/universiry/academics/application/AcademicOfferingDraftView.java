package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOfferingDraft;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;

import java.time.Instant;

public record AcademicOfferingDraftView(
        AcademicOfferingDraft draft,
        String periodCode,
        AcademicPeriodKind periodKind,
        String programCode,
        String programName,
        String curriculumVersion,
        String subjectCode,
        String subjectName,
        String sourceReference,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
