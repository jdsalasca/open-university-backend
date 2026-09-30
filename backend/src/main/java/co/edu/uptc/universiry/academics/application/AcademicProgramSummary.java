package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;

import java.util.UUID;

public record AcademicProgramSummary(
        UUID id,
        String programCode,
        AcademicLevel academicLevel,
        StudyModality studyModality,
        String campusCode,
        String programName,
        String faculty,
        String campusName
) {
}
