package co.edu.uptc.universiry.academics.domain;

import java.util.UUID;

public record AcademicSubject(UUID id, String subjectCode) {
    public AcademicSubject {
        id = AcademicCatalogValueRules.requiredId(id, "subject.id");
        subjectCode = AcademicCatalogValueRules.identifier(
                subjectCode, AcademicCatalogLimits.MAX_SUBJECT_CODE_LENGTH, "subject.code");
    }
}
