package co.edu.uptc.universiry.academics.domain;

import java.util.UUID;

public record AcademicCurriculumEntry(
        UUID subjectId,
        AcademicSubjectRevision subjectRevision,
        int semester,
        String formationSpace,
        String component,
        String choiceGroup,
        int rowOrder
) {
    public AcademicCurriculumEntry {
        subjectId = AcademicCatalogValueRules.requiredId(subjectId, "curriculumEntry.subjectId");
        if (subjectRevision == null) {
            throw AcademicCatalogValueRules.invalid("curriculumEntry.subjectRevision", "is required");
        }
        if (!subjectId.equals(subjectRevision.subjectId())) {
            throw AcademicCatalogValueRules.invalid("curriculumEntry.subjectRevision", "must belong to its subject");
        }
        if (semester < 1 || semester > AcademicCatalogLimits.MAX_SEMESTER_NUMBER) {
            throw AcademicCatalogValueRules.invalid("curriculumEntry.semester", "is outside the supported range");
        }
        formationSpace = AcademicCatalogValueRules.requiredText(
                formationSpace, AcademicCatalogLimits.MAX_FORMATION_SPACE_LENGTH, "curriculumEntry.formationSpace");
        component = AcademicCatalogValueRules.requiredText(
                component, AcademicCatalogLimits.MAX_COMPONENT_LENGTH, "curriculumEntry.component");
        choiceGroup = AcademicCatalogValueRules.optionalText(
                choiceGroup, AcademicCatalogLimits.MAX_CHOICE_GROUP_LENGTH, "curriculumEntry.choiceGroup");
        if (rowOrder < 1 || rowOrder > AcademicCatalogLimits.MAX_IMPORT_ROWS) {
            throw AcademicCatalogValueRules.invalid("curriculumEntry.rowOrder", "is outside the import row range");
        }
    }
}
