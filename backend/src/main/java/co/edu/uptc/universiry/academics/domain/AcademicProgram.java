package co.edu.uptc.universiry.academics.domain;

import java.util.UUID;

public record AcademicProgram(
        UUID id,
        String programCode,
        AcademicLevel academicLevel,
        StudyModality studyModality,
        String campusCode
) {
    public AcademicProgram {
        id = AcademicCatalogValueRules.requiredId(id, "program.id");
        programCode = AcademicCatalogValueRules.identifier(
                programCode, AcademicCatalogLimits.MAX_PROGRAM_CODE_LENGTH, "program.code");
        if (academicLevel == null) {
            throw AcademicCatalogValueRules.invalid("program.academicLevel", "is required");
        }
        if (studyModality == null) {
            throw AcademicCatalogValueRules.invalid("program.studyModality", "is required");
        }
        campusCode = AcademicCatalogValueRules.identifier(
                campusCode, AcademicCatalogLimits.MAX_CAMPUS_CODE_LENGTH, "program.campusCode");
    }
}
