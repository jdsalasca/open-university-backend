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
        List<Entry> sampleEntries,
        CurriculumVersionComparisonResponse comparison
) {

    public static AcademicCurriculumImportPreviewResponse from(CurriculumImportPreview preview) {
        return new AcademicCurriculumImportPreviewResponse(
                preview.programCode(), preview.academicLevel().name(), preview.studyModality().name(), preview.sniesCode(),
                preview.programName(),
                preview.faculty(), preview.campusCode(), preview.campusName(), preview.curriculumVersion(),
                preview.cohortFrom(), preview.cohortThrough(), preview.approvalReference(), preview.entryCount(),
                preview.semesters(), preview.sampleEntries().stream().map(Entry::from).toList(),
                CurriculumVersionComparisonResponse.from(preview.comparison()));
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

    public record CurriculumVersionComparisonResponse(
            String status,
            Reference reference,
            Counts counts,
            List<Sample> addedSamples,
            List<Sample> removedSamples,
            List<Sample> modifiedSamples,
            List<Sample> unchangedSamples
    ) {
        private static CurriculumVersionComparisonResponse from(
                co.edu.uptc.universiry.academics.application.CurriculumVersionComparison comparison
        ) {
            return new CurriculumVersionComparisonResponse(
                    comparison.status().name(),
                    comparison.reference() == null ? null : Reference.from(comparison.reference()),
                    comparison.counts() == null ? null : Counts.from(comparison.counts()),
                    comparison.addedSamples().stream().map(Sample::from).toList(),
                    comparison.removedSamples().stream().map(Sample::from).toList(),
                    comparison.modifiedSamples().stream().map(Sample::from).toList(),
                    comparison.unchangedSamples().stream().map(Sample::from).toList());
        }
    }

    public record Reference(
            java.util.UUID curriculumId,
            String curriculumVersion,
            String cohortFrom,
            String cohortThrough,
            java.time.Instant publishedAt
    ) {
        private static Reference from(
                co.edu.uptc.universiry.academics.application.CurriculumVersionComparison.Reference reference
        ) {
            return new Reference(reference.curriculumId(), reference.curriculumVersion(), reference.cohortFrom(),
                    reference.cohortThrough(), reference.publishedAt());
        }
    }

    public record Counts(int added, int removed, int modified, int unchanged) {
        private static Counts from(
                co.edu.uptc.universiry.academics.application.CurriculumVersionComparison.Counts counts
        ) {
            return new Counts(counts.added(), counts.removed(), counts.modified(), counts.unchanged());
        }
    }

    public record Sample(String subjectCode, List<String> changedFields) {
        private static Sample from(
                co.edu.uptc.universiry.academics.application.CurriculumVersionComparison.Sample sample
        ) {
            return new Sample(sample.subjectCode(), sample.changedFields().stream().map(Enum::name).toList());
        }
    }
}
