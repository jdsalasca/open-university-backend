package co.edu.uptc.universiry.academics.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record AcademicCurriculum(
        UUID id,
        UUID programId,
        AcademicProgramRevision programRevision,
        String curriculumVersion,
        String cohortFrom,
        String cohortThrough,
        String approvalReference,
        AcademicCurriculumStatus status,
        List<AcademicCurriculumEntry> entries
) {
    public AcademicCurriculum {
        id = AcademicCatalogValueRules.requiredId(id, "curriculum.id");
        programId = AcademicCatalogValueRules.requiredId(programId, "curriculum.programId");
        if (programRevision == null) {
            throw AcademicCatalogValueRules.invalid("curriculum.programRevision", "is required");
        }
        if (!programId.equals(programRevision.programId())) {
            throw AcademicCatalogValueRules.invalid("curriculum.programRevision", "must belong to its program");
        }
        curriculumVersion = AcademicCatalogValueRules.requiredText(
                curriculumVersion, AcademicCatalogLimits.MAX_CURRICULUM_VERSION_LENGTH, "curriculum.version");

        AcademicCatalogValueRules.CohortTerm start = AcademicCatalogValueRules.cohortTerm(cohortFrom, "curriculum.cohortFrom");
        cohortFrom = String.format(Locale.ROOT, "%04d-%d", start.year(), start.term());
        if (cohortThrough == null || cohortThrough.isBlank()) {
            cohortThrough = null;
        } else {
            AcademicCatalogValueRules.CohortTerm end = AcademicCatalogValueRules.cohortTerm(cohortThrough, "curriculum.cohortThrough");
            if (end.compareTo(start) < 0) {
                throw AcademicCatalogValueRules.invalid("curriculum.cohortThrough", "cannot precede cohortFrom");
            }
            cohortThrough = String.format(Locale.ROOT, "%04d-%d", end.year(), end.term());
        }
        approvalReference = AcademicCatalogValueRules.requiredText(
                approvalReference, AcademicCatalogLimits.MAX_APPROVAL_REFERENCE_LENGTH, "curriculum.approvalReference");
        if (status == null) {
            throw AcademicCatalogValueRules.invalid("curriculum.status", "is required");
        }
        if (entries == null || entries.isEmpty() || entries.size() > AcademicCatalogLimits.MAX_CURRICULUM_ENTRIES) {
            throw AcademicCatalogValueRules.invalid(
                    "curriculum.entries", "must contain between 1 and "
                            + AcademicCatalogLimits.MAX_CURRICULUM_ENTRIES + " entries");
        }

        HashSet<UUID> subjectIds = new HashSet<>();
        HashSet<Integer> rowOrders = new HashSet<>();
        for (AcademicCurriculumEntry entry : entries) {
            if (entry == null) {
                throw AcademicCatalogValueRules.invalid("curriculum.entries", "cannot contain null entries");
            }
            if (!subjectIds.add(entry.subjectId())) {
                throw AcademicCatalogValueRules.invalid("curriculum.entries", "cannot repeat a subject");
            }
            if (!rowOrders.add(entry.rowOrder())) {
                throw AcademicCatalogValueRules.invalid("curriculum.entries", "cannot repeat a source row order");
            }
        }
        entries = List.copyOf(entries);
    }

    public AcademicCurriculum publish() {
        if (status != AcademicCurriculumStatus.DRAFT) {
            throw new IllegalStateException("Only a draft curriculum can be published.");
        }
        return new AcademicCurriculum(
                id,
                programId,
                programRevision,
                curriculumVersion,
                cohortFrom,
                cohortThrough,
                approvalReference,
                AcademicCurriculumStatus.PUBLISHED,
                entries
        );
    }
}
