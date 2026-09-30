package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;

import java.math.BigDecimal;
import java.util.List;

public record CurriculumImportPreview(
        String programCode,
        AcademicLevel academicLevel,
        StudyModality studyModality,
        String sniesCode,
        String programName,
        String faculty,
        String campusCode,
        String campusName,
        String curriculumVersion,
        String cohortFrom,
        String cohortThrough,
        String approvalReference,
        int entryCount,
        List<Integer> semesters,
        List<Entry> sampleEntries
) {

    private static final int MAX_SAMPLE_ENTRIES = 10;

    public CurriculumImportPreview {
        semesters = List.copyOf(semesters);
        sampleEntries = List.copyOf(sampleEntries);
        if (entryCount < 1 || sampleEntries.size() > Math.min(MAX_SAMPLE_ENTRIES, entryCount) || semesters.isEmpty()) {
            throw new IllegalArgumentException("The curriculum preview is outside its validated bounds.");
        }
    }

    public static CurriculumImportPreview from(ValidatedCurriculum curriculum) {
        ValidatedProgram program = curriculum.program();
        List<ValidatedCurriculumEntry> entries = curriculum.entries();
        List<Integer> semesters = entries.stream()
                .map(ValidatedCurriculumEntry::semester)
                .distinct()
                .sorted()
                .toList();
        List<Entry> sample = entries.stream()
                .limit(MAX_SAMPLE_ENTRIES)
                .map(Entry::from)
                .toList();

        return new CurriculumImportPreview(
                program.programCode(),
                program.academicLevel(),
                program.studyModality(),
                program.sniesCode(),
                program.programName(),
                program.faculty(),
                program.campusCode(),
                program.campusName(),
                curriculum.curriculumVersion(),
                curriculum.cohortFrom(),
                curriculum.cohortThrough(),
                curriculum.approvalReference(),
                entries.size(),
                semesters,
                sample);
    }

    public record Entry(
            int sourceRowNumber,
            int rowOrder,
            int semester,
            String subjectCode,
            String subjectName,
            BigDecimal credits,
            String formationSpace,
            String component,
            String choiceGroup
    ) {
        private static Entry from(ValidatedCurriculumEntry entry) {
            return new Entry(
                    entry.sourceRowNumber(), entry.rowOrder(), entry.semester(), entry.subjectCode(),
                    entry.subjectName(), entry.credits(), entry.formationSpace(), entry.component(), entry.choiceGroup());
        }
    }
}
