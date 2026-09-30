package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.CurriculumImportPreview;

import java.math.BigDecimal;
import java.util.List;

public record AcademicCurriculumImportPreviewResponse(
        String programCode,
        String academicLevel,
        String studyModality,
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

    public static AcademicCurriculumImportPreviewResponse from(CurriculumImportPreview preview) {
        return new AcademicCurriculumImportPreviewResponse(
                preview.programCode(), preview.academicLevel().name(), preview.studyModality().name(), preview.sniesCode(),
                preview.programName(),
                preview.faculty(), preview.campusCode(), preview.campusName(), preview.curriculumVersion(),
                preview.cohortFrom(), preview.cohortThrough(), preview.approvalReference(), preview.entryCount(),
                preview.semesters(), preview.sampleEntries().stream().map(Entry::from).toList());
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
        private static Entry from(CurriculumImportPreview.Entry entry) {
            return new Entry(
                    entry.sourceRowNumber(), entry.rowOrder(), entry.semester(), entry.subjectCode(), entry.subjectName(),
                    entry.credits(), entry.formationSpace(), entry.component(), entry.choiceGroup());
        }
    }
}
