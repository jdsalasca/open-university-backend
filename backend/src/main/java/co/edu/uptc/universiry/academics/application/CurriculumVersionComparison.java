package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;
import co.edu.uptc.universiry.academics.domain.AcademicCatalogValueRules;
import co.edu.uptc.universiry.academics.domain.AcademicCurriculumStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
        status = Objects.requireNonNull(status, "status");
        addedSamples = List.copyOf(addedSamples);
        removedSamples = List.copyOf(removedSamples);
        modifiedSamples = List.copyOf(modifiedSamples);
        unchangedSamples = List.copyOf(unchangedSamples);

        if (status == Status.NO_REFERENCE) {
            if (reference != null || counts != null || !addedSamples.isEmpty() || !removedSamples.isEmpty()
                    || !modifiedSamples.isEmpty() || !unchangedSamples.isEmpty()) {
                throw new IllegalArgumentException("A comparison without a reference cannot contain results.");
            }
        } else {
            Objects.requireNonNull(reference, "reference");
            Objects.requireNonNull(counts, "counts");
            requireSampleBounds(addedSamples, counts.added());
            requireSampleBounds(removedSamples, counts.removed());
            requireSampleBounds(modifiedSamples, counts.modified());
            requireSampleBounds(unchangedSamples, counts.unchanged());
            requireFieldlessSamples(addedSamples);
            requireFieldlessSamples(removedSamples);
            requireChangedSamples(modifiedSamples);
            requireFieldlessSamples(unchangedSamples);
        }
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

        CurriculumSummary summary = Objects.requireNonNull(reference.curriculum(), "reference.curriculum");
        if (summary.status() != AcademicCurriculumStatus.PUBLISHED || summary.publishedAt() == null) {
            throw new IllegalArgumentException("The comparison reference must be a published curriculum.");
        }
        if (!sameProgram(incoming.program(), summary)) {
            throw new IllegalArgumentException("The comparison reference must match the incoming program identity.");
        }

        List<AcademicCurriculumEntrySummary> referenceEntries = List.copyOf(reference.entries());
        if (summary.entryCount() != referenceEntries.size()) {
            throw new IllegalArgumentException("The published curriculum entry count does not match its detail.");
        }

        Map<String, ValidatedCurriculumEntry> incomingByCode = indexIncoming(incoming.entries());
        Map<String, AcademicCurriculumEntrySummary> referenceByCode = indexReference(referenceEntries);

        List<Sample> added = new ArrayList<>();
        List<Sample> modified = new ArrayList<>();
        List<Sample> unchanged = new ArrayList<>();
        for (ValidatedCurriculumEntry entry : incoming.entries()) {
            String code = canonicalSubjectCode(entry.subjectCode());
            AcademicCurriculumEntrySummary previous = referenceByCode.get(code);
            if (previous == null) {
                added.add(sample(code));
                continue;
            }
            List<ChangedField> fields = changedFields(entry, previous);
            (fields.isEmpty() ? unchanged : modified).add(new Sample(code, fields));
        }

        List<Sample> removed = referenceEntries.stream()
                .filter(entry -> !incomingByCode.containsKey(canonicalSubjectCode(entry.subjectCode())))
                .map(entry -> sample(canonicalSubjectCode(entry.subjectCode())))
                .toList();

        Reference metadata = new Reference(summary.id(), summary.curriculumVersion(), summary.cohortFrom(),
                summary.cohortThrough(), summary.publishedAt());
        Counts counts = new Counts(added.size(), removed.size(), modified.size(), unchanged.size());
        return new CurriculumVersionComparison(Status.COMPARED, metadata, counts,
                bounded(added), bounded(removed), bounded(modified), bounded(unchanged));
    }

    private static Map<String, ValidatedCurriculumEntry> indexIncoming(List<ValidatedCurriculumEntry> entries) {
        Map<String, ValidatedCurriculumEntry> result = new LinkedHashMap<>();
        for (ValidatedCurriculumEntry entry : List.copyOf(entries)) {
            String code = canonicalSubjectCode(entry.subjectCode());
            if (result.putIfAbsent(code, entry) != null) {
                throw new IllegalArgumentException("Incoming curriculum subject codes must be unique.");
            }
        }
        return result;
    }

    private static Map<String, AcademicCurriculumEntrySummary> indexReference(
            List<AcademicCurriculumEntrySummary> entries
    ) {
        Map<String, AcademicCurriculumEntrySummary> result = new LinkedHashMap<>();
        for (AcademicCurriculumEntrySummary entry : entries) {
            String code = canonicalSubjectCode(entry.subjectCode());
            if (result.putIfAbsent(code, entry) != null) {
                throw new IllegalArgumentException("Published curriculum subject codes must be unique.");
            }
        }
        return result;
    }

    private static String canonicalSubjectCode(String subjectCode) {
        return AcademicCatalogValueRules.identifier(
                subjectCode, AcademicCatalogLimits.MAX_SUBJECT_CODE_LENGTH, "subjectCode");
    }

    private static boolean sameProgram(ValidatedProgram incoming, CurriculumSummary reference) {
        CurriculumProgramIdentity incomingIdentity = CurriculumProgramIdentity.from(incoming);
        CurriculumProgramIdentity referenceIdentity = new CurriculumProgramIdentity(
                reference.programCode(), reference.academicLevel(), reference.studyModality(), reference.campusCode());
        return incomingIdentity.equals(referenceIdentity);
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

    private static Sample sample(String code) {
        return new Sample(code, List.of());
    }

    private static List<Sample> bounded(List<Sample> samples) {
        return samples.stream().limit(MAX_SAMPLES_PER_CATEGORY).toList();
    }

    private static void requireSampleBounds(List<Sample> samples, int count) {
        if (samples.size() > Math.min(MAX_SAMPLES_PER_CATEGORY, count)) {
            throw new IllegalArgumentException("A curriculum comparison contains too many samples.");
        }
    }

    private static void requireFieldlessSamples(List<Sample> samples) {
        if (samples.stream().anyMatch(sample -> !sample.changedFields().isEmpty())) {
            throw new IllegalArgumentException("Only modified curriculum samples can contain changed fields.");
        }
    }

    private static void requireChangedSamples(List<Sample> samples) {
        if (samples.stream().anyMatch(sample -> sample.changedFields().isEmpty())) {
            throw new IllegalArgumentException("Modified curriculum samples must contain changed fields.");
        }
    }

    private static void requireDistinctFields(List<ChangedField> fields) {
        Set<ChangedField> distinct = new LinkedHashSet<>(fields);
        if (distinct.size() != fields.size()) {
            throw new IllegalArgumentException("A curriculum sample cannot repeat a changed field.");
        }
    }

    public enum Status {
        NO_REFERENCE,
        COMPARED
    }

    public record Reference(
            UUID curriculumId,
            String curriculumVersion,
            String cohortFrom,
            String cohortThrough,
            Instant publishedAt
    ) {
        public Reference {
            Objects.requireNonNull(curriculumId, "curriculumId");
            Objects.requireNonNull(curriculumVersion, "curriculumVersion");
            Objects.requireNonNull(cohortFrom, "cohortFrom");
            Objects.requireNonNull(publishedAt, "publishedAt");
        }
    }

    public record Counts(int added, int removed, int modified, int unchanged) {
        public Counts {
            if (added < 0 || removed < 0 || modified < 0 || unchanged < 0) {
                throw new IllegalArgumentException("Curriculum comparison counts cannot be negative.");
            }
        }
    }

    public record Sample(String subjectCode, List<ChangedField> changedFields) {
        public Sample {
            subjectCode = canonicalSubjectCode(subjectCode);
            changedFields = List.copyOf(changedFields);
            requireDistinctFields(changedFields);
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
