package co.edu.uptc.universiry.academics.application;

import java.util.List;
import java.util.Objects;

public final class CurriculumCsvException extends RuntimeException {

    public enum Code {
        EMPTY_FILE,
        INVALID_UTF8,
        INVALID_HEADERS,
        MALFORMED_CSV,
        FILE_TOO_LARGE,
        TOO_MANY_ROWS,
        INVALID_DATA
    }

    private final Code code;
    private final List<CurriculumCsvIssue> issues;

    public CurriculumCsvException(Code code, List<CurriculumCsvIssue> issues) {
        super(Objects.requireNonNull(code, "code").name());
        this.code = code;
        this.issues = List.copyOf(issues);
    }

    public Code code() {
        return code;
    }

    public List<CurriculumCsvIssue> issues() {
        return issues;
    }
}
