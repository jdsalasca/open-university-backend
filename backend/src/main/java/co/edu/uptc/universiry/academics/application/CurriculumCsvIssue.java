package co.edu.uptc.universiry.academics.application;

import java.util.Objects;

public record CurriculumCsvIssue(Integer rowNumber, String column, Code code) {

    public CurriculumCsvIssue {
        Objects.requireNonNull(code, "code");
    }

    public enum Code {
        INVALID_ROW_WIDTH,
        REQUIRED_VALUE_MISSING,
        FIELD_TOO_LONG,
        INVALID_IDENTIFIER,
        UNSUPPORTED_ACADEMIC_LEVEL,
        UNSUPPORTED_STUDY_MODALITY,
        INVALID_CREDITS,
        INVALID_SEMESTER,
        DUPLICATE_SUBJECT_CODE,
        INCONSISTENT_METADATA,
        INVALID_COHORT_RANGE,
        INVALID_VALUE
    }
}
