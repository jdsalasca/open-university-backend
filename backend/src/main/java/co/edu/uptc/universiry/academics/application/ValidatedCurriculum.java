package co.edu.uptc.universiry.academics.application;

import java.util.List;

public record ValidatedCurriculum(
        ValidatedProgram program,
        String curriculumVersion,
        String cohortFrom,
        String cohortThrough,
        String approvalReference,
        List<ValidatedCurriculumEntry> entries,
        String sourceSha256
) {

    public ValidatedCurriculum {
        entries = List.copyOf(entries);
    }
}
