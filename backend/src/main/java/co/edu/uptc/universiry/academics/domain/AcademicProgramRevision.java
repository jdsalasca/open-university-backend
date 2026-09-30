package co.edu.uptc.universiry.academics.domain;

import java.util.UUID;

public record AcademicProgramRevision(
        UUID id,
        UUID programId,
        String sniesCode,
        String programName,
        String faculty,
        String campusName
) {
    public AcademicProgramRevision {
        id = AcademicCatalogValueRules.requiredId(id, "programRevision.id");
        programId = AcademicCatalogValueRules.requiredId(programId, "programRevision.programId");
        sniesCode = AcademicCatalogValueRules.optionalText(
                sniesCode, AcademicCatalogLimits.MAX_SNIES_CODE_LENGTH, "programRevision.sniesCode");
        programName = AcademicCatalogValueRules.requiredText(
                programName, AcademicCatalogLimits.MAX_PROGRAM_NAME_LENGTH, "programRevision.programName");
        faculty = AcademicCatalogValueRules.requiredText(
                faculty, AcademicCatalogLimits.MAX_FACULTY_LENGTH, "programRevision.faculty");
        campusName = AcademicCatalogValueRules.requiredText(
                campusName, AcademicCatalogLimits.MAX_CAMPUS_NAME_LENGTH, "programRevision.campusName");
    }
}
