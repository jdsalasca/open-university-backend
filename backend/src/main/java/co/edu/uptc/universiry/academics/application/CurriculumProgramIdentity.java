package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;

import java.util.Objects;

public record CurriculumProgramIdentity(
        String programCode,
        AcademicLevel academicLevel,
        StudyModality studyModality,
        String campusCode
) {

    public CurriculumProgramIdentity {
        programCode = requireCode(programCode, "programCode");
        academicLevel = Objects.requireNonNull(academicLevel, "academicLevel");
        studyModality = Objects.requireNonNull(studyModality, "studyModality");
        campusCode = requireCode(campusCode, "campusCode");
    }

    public static CurriculumProgramIdentity from(ValidatedProgram program) {
        Objects.requireNonNull(program, "program");
        return new CurriculumProgramIdentity(program.programCode(), program.academicLevel(),
                program.studyModality(), program.campusCode());
    }

    private static String requireCode(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.strip();
    }
}
