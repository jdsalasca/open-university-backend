package co.edu.uptc.universiry.academics.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record AcademicSubjectRevision(UUID id, UUID subjectId, String subjectName, BigDecimal credits) {
    public AcademicSubjectRevision {
        id = AcademicCatalogValueRules.requiredId(id, "subjectRevision.id");
        subjectId = AcademicCatalogValueRules.requiredId(subjectId, "subjectRevision.subjectId");
        subjectName = AcademicCatalogValueRules.requiredText(
                subjectName, AcademicCatalogLimits.MAX_SUBJECT_NAME_LENGTH, "subjectRevision.subjectName");
        credits = AcademicCatalogValueRules.credits(credits);
    }
}
