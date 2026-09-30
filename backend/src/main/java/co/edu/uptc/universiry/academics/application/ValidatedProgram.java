package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;

public record ValidatedProgram(
        String programCode,
        AcademicLevel academicLevel,
        StudyModality studyModality,
        String sniesCode,
        String programName,
        String faculty,
        String campusCode,
        String campusName
) {
}
