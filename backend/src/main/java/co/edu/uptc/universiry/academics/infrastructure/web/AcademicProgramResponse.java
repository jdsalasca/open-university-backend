package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicProgramSummary;
import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;

import java.util.UUID;

public record AcademicProgramResponse(
        UUID id,
        String programCode,
        AcademicLevel academicLevel,
        StudyModality studyModality,
        String campusCode,
        String programName,
        String faculty,
        String campusName
) {

    public static AcademicProgramResponse from(AcademicProgramSummary summary) {
        return new AcademicProgramResponse(
                summary.id(), summary.programCode(), summary.academicLevel(), summary.studyModality(),
                summary.campusCode(), summary.programName(), summary.faculty(), summary.campusName());
    }
}
