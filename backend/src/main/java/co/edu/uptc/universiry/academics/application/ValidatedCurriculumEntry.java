package co.edu.uptc.universiry.academics.application;

import java.math.BigDecimal;

public record ValidatedCurriculumEntry(
        int sourceRowNumber,
        int rowOrder,
        int semester,
        String subjectCode,
        String subjectName,
        BigDecimal credits,
        String formationSpace,
        String component,
        String choiceGroup
) {
}
