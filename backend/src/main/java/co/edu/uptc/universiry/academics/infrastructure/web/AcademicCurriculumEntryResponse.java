package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCurriculumEntrySummary;

import java.math.BigDecimal;
import java.util.UUID;

public record AcademicCurriculumEntryResponse(
        UUID subjectId,
        UUID subjectRevisionId,
        String subjectCode,
        String subjectName,
        BigDecimal credits,
        int semester,
        String formationSpace,
        String component,
        String choiceGroup,
        int rowOrder
) {

    public static AcademicCurriculumEntryResponse from(AcademicCurriculumEntrySummary summary) {
        return new AcademicCurriculumEntryResponse(
                summary.subjectId(), summary.subjectRevisionId(), summary.subjectCode(), summary.subjectName(),
                summary.credits(), summary.semester(), summary.formationSpace(), summary.component(),
                summary.choiceGroup(), summary.rowOrder());
    }
}
