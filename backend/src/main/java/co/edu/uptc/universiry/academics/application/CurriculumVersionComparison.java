package co.edu.uptc.universiry.academics.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record CurriculumVersionComparison(
        Status status,
        Reference reference,
        Counts counts,
        List<Sample> addedSamples,
        List<Sample> removedSamples,
        List<Sample> modifiedSamples,
        List<Sample> unchangedSamples
) {

    private static final int MAX_SAMPLES_PER_CATEGORY = 10;

    public CurriculumVersionComparison {
        addedSamples = List.copyOf(addedSamples);
        removedSamples = List.copyOf(removedSamples);
        modifiedSamples = List.copyOf(modifiedSamples);
        unchangedSamples = List.copyOf(unchangedSamples);
    }

    public static CurriculumVersionComparison noReference() {
        return new CurriculumVersionComparison(Status.NO_REFERENCE, null, null,
                List.of(), List.of(), List.of(), List.of());
    }

    public static CurriculumVersionComparison compare(
            ValidatedCurriculum incoming,
            AcademicCurriculumDetails reference
    ) {
        Objects.requireNonNull(incoming, "incoming");
        Objects.requireNonNull(reference, "reference");

        Map<String, ValidatedCurriculumEntry> incomingByCode = new LinkedHashMap<>();
        incoming.entries().forEach(entry -> incomingByCode.put(normalizeCode(entry.subjectCode()), entry));
        Map<String, AcademicCurriculumEntrySummary> referenceByCode = new LinkedHashMap<>();
        reference.entries().forEach(entry -> referenceByCode.put(normalizeCode(entry.subjectCode()), entry));

        List<Sample> added = new ArrayList<>();
        List<Sample> modified = new ArrayList<>();
        List<Sample> unchanged = new ArrayList<>();
        for (ValidatedCurriculumEntry entry : incoming.entries()) {
            String code = normalizeCode(entry.subjectCode());
            AcademicCurriculumEntrySummary previous = referenceByCode.get(code);
            if (previous == null) {
                added.add(sample(code));
                continue;
            }
            List<ChangedField> fields = changedFields(entry, previous);
            (fields.isEmpty() ? unchanged : modified).add(new Sample(code, fields));
        }

        List<Sample> removed = reference.entries().stream()
                .filter(entry -> !incomingByCode.containsKey(normalizeCode(entry.subjectCode())))
                .map(entry -> sample(normalizeCode(entry.subjectCode())))
                .toList();

        CurriculumSummary summary = reference.curriculum();
        Reference metadata = new Reference(summary.id(), summary.curriculumVersion(), summary.cohortFrom(),
                summary.cohortThrough(), summary.publishedAt());
        Counts counts = new Counts(added.size(), removed.size(), modified.size(), unchanged.size());
        return new CurriculumVersionComparison(Status.COMPARED, metadata, counts,
                bounded(added), bounded(removed), bounded(modified), bounded(unchanged));
    }

    private static List<ChangedField> changedFields(
            ValidatedCurriculumEntry incoming,
            AcademicCurriculumEntrySummary reference
    ) {
        List<ChangedField> fields = new ArrayList<>();
        if (!Objects.equals(incoming.subjectName(), reference.subjectName())) fields.add(ChangedField.NAME);
        if (incoming.credits().compareTo(reference.credits()) != 0) fields.add(ChangedField.CREDITS);
        if (incoming.semester() != reference.semester()) fields.add(ChangedField.SEMESTER);
        if (incoming.rowOrder() != reference.rowOrder()) fields.add(ChangedField.ORDER);
        if (!Objects.equals(incoming.formationSpace(), reference.formationSpace())) fields.add(ChangedField.FORMATION_SPACE);
        if (!Objects.equals(incoming.component(), reference.component())) fields.add(ChangedField.COMPONENT);
        if (!Objects.equals(incoming.choiceGroup(), reference.choiceGroup())) fields.add(ChangedField.CHOICE_GROUP);
        return List.copyOf(fields);
    }

    private static String normalizeCode(String subjectCode) {
        return subjectCode.strip().toUpperCase(Locale.ROOT);
    }

    private static Sample sample(String code) {
        return new Sample(code, List.of());
    }

    private static List<Sample> bounded(List<Sample> samples) {
        return samples.stream().limit(MAX_SAMPLES_PER_CATEGORY).toList();
    }

    public enum Status {
        NO_REFERENCE,
        COMPARED
    }

    public record Reference(
            java.util.UUID curriculumId,
            String curriculumVersion,
            String cohortFrom,
            String cohortThrough,
            java.time.Instant publishedAt
    ) {
    }

    public record Counts(int added, int removed, int modified, int unchanged) {
    }

    public record Sample(String subjectCode, List<ChangedField> changedFields) {
        public Sample {
            changedFields = List.copyOf(changedFields);
        }
    }

    public enum ChangedField {
        NAME,
        CREDITS,
        SEMESTER,
        ORDER,
        FORMATION_SPACE,
        COMPONENT,
        CHOICE_GROUP
    }
}
