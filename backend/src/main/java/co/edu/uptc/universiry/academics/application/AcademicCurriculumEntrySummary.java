package co.edu.uptc.universiry.academics.application;

import java.math.BigDecimal;
import java.util.UUID;

public record AcademicCurriculumEntrySummary(
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
}
