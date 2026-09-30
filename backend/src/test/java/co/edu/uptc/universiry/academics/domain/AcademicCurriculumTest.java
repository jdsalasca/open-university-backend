package co.edu.uptc.universiry.academics.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademicCurriculumTest {

    @Test
    void rejects_invalid_cohort_terms_and_reversed_ranges() {
        // Arrange
        UUID programId = UUID.randomUUID();
        AcademicProgramRevision programRevision = programRevision(programId);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision, "2026-3", "", List.of(entry(UUID.randomUUID(), 1))));
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision, "2026-1", "2025-2", List.of(entry(UUID.randomUUID(), 1))));
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision, "2026-1", "2026-1-extra", List.of(entry(UUID.randomUUID(), 1))));
    }

    @Test
    void rejects_missing_or_mismatched_program_revision_and_empty_entries() {
        // Arrange
        UUID programId = UUID.randomUUID();
        AcademicProgramRevision revisionForOtherProgram = programRevision(UUID.randomUUID());
        List<AcademicCurriculumEntry> nullEntry = new ArrayList<>();
        nullEntry.add(null);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, revisionForOtherProgram, "2026-1", "", List.of(entry(UUID.randomUUID(), 1))));
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision(programId), "2026-1", "", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision(programId), "2026-1", "", null));
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision(programId), "2026-1", "", nullEntry));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCurriculum(UUID.randomUUID(), programId, programRevision(programId), " ",
                        "2026-1", null, "Referencia", AcademicCurriculumStatus.DRAFT, List.of(entry(UUID.randomUUID(), 1))));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCurriculum(UUID.randomUUID(), programId, programRevision(programId), "V".repeat(81),
                        "2026-1", null, "Referencia", AcademicCurriculumStatus.DRAFT, List.of(entry(UUID.randomUUID(), 1))));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCurriculum(UUID.randomUUID(), programId, programRevision(programId), "PLAN-2026",
                        "2026-1", null, " ", AcademicCurriculumStatus.DRAFT, List.of(entry(UUID.randomUUID(), 1))));
    }

    @Test
    void rejects_duplicate_subject_identity_even_when_revisions_differ() {
        // Arrange
        UUID programId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        AcademicSubjectRevision firstRevision = subjectRevision(subjectId, "Álgebra", "3");
        AcademicSubjectRevision secondRevision = subjectRevision(subjectId, "Álgebra lineal", "4");
        List<AcademicCurriculumEntry> entries = List.of(
                entry(subjectId, firstRevision, 1),
                entry(subjectId, secondRevision, 2)
        );

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision(programId), "2026-1", "", entries));
    }

    @Test
    void rejects_mismatched_subject_revision_and_duplicate_row_order() {
        // Arrange
        UUID programId = UUID.randomUUID();
        UUID firstSubject = UUID.randomUUID();
        UUID secondSubject = UUID.randomUUID();
        AcademicSubjectRevision revisionForFirst = subjectRevision(firstSubject, "Álgebra", "3");

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> entry(secondSubject, revisionForFirst, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCurriculumEntry(firstSubject, revisionForFirst, 0, "Básico", "Disciplinar", null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCurriculumEntry(firstSubject, revisionForFirst, 32_768, "Básico", "Disciplinar", null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCurriculumEntry(firstSubject, revisionForFirst, 1, "F".repeat(121), "Disciplinar", null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCurriculumEntry(firstSubject, revisionForFirst, 1, "Básico", "Disciplinar", null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new AcademicCurriculumEntry(firstSubject, revisionForFirst, 1, "Básico", "Disciplinar", null, 10_001));
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision(programId), "2026-1", "", List.of(
                        entry(firstSubject, revisionForFirst, 1),
                        entry(secondSubject, subjectRevision(secondSubject, "Cálculo", "4"), 1)
                )));
    }

    @Test
    void rejects_a_curriculum_over_the_maximum_entry_count() {
        // Arrange
        UUID programId = UUID.randomUUID();
        List<AcademicCurriculumEntry> entries = IntStream.rangeClosed(1, 10_001)
                .mapToObj(rowOrder -> entry(UUID.randomUUID(), ((rowOrder - 1) % 10_000) + 1))
                .toList();

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> curriculum(programId, programRevision(programId), "2026-1", "", entries));
    }

    @Test
    void publishes_a_draft_as_a_new_immutable_curriculum_once() {
        // Arrange
        UUID programId = UUID.randomUUID();
        AcademicCurriculum draft = curriculum(
                programId,
                programRevision(programId),
                "2026-1",
                "",
                List.of(entry(UUID.randomUUID(), 1))
        );

        // Act
        AcademicCurriculum published = draft.publish();

        // Assert
        assertEquals(AcademicCurriculumStatus.DRAFT, draft.status());
        assertEquals(AcademicCurriculumStatus.PUBLISHED, published.status());
        assertEquals(draft.id(), published.id());
        assertThrows(IllegalStateException.class, published::publish);
    }

    @Test
    void accepts_elective_group_labels_and_keeps_entries_immutable() {
        // Arrange
        UUID programId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        List<AcademicCurriculumEntry> mutableEntries = new ArrayList<>(List.of(
                entry(subjectId, subjectRevision(subjectId, "Seminario, arte y cultura", "2"), 1, "Electiva libre")
        ));

        // Act
        AcademicCurriculum curriculum = curriculum(
                programId, programRevision(programId), "2026-1", "2027-2", mutableEntries);
        mutableEntries.clear();

        // Assert
        assertEquals(AcademicCurriculumStatus.DRAFT, curriculum.status());
        assertEquals(1, curriculum.entries().size());
        assertEquals("Electiva libre", curriculum.entries().getFirst().choiceGroup());
        assertEquals("2026-1", curriculum.cohortFrom());
        assertEquals("2027-2", curriculum.cohortThrough());
        assertThrows(UnsupportedOperationException.class,
                () -> curriculum.entries().add(entry(UUID.randomUUID(), 2)));
    }

    private AcademicCurriculum curriculum(
            UUID programId,
            AcademicProgramRevision programRevision,
            String cohortFrom,
            String cohortThrough,
            List<AcademicCurriculumEntry> entries
    ) {
        return new AcademicCurriculum(
                UUID.randomUUID(),
                programId,
                programRevision,
                "PLAN-2026",
                cohortFrom,
                cohortThrough,
                "Acuerdo 000 de 2026",
                AcademicCurriculumStatus.DRAFT,
                entries
        );
    }

    private AcademicProgramRevision programRevision(UUID programId) {
        return new AcademicProgramRevision(
                UUID.randomUUID(), programId, "12345", "Ingeniería de prueba", "Facultad de prueba", "Tunja");
    }

    private AcademicCurriculumEntry entry(UUID subjectId, int rowOrder) {
        return entry(subjectId, subjectRevision(subjectId, "Asignatura", "3"), rowOrder);
    }

    private AcademicCurriculumEntry entry(UUID subjectId, AcademicSubjectRevision revision, int rowOrder) {
        return entry(subjectId, revision, rowOrder, null);
    }

    private AcademicCurriculumEntry entry(UUID subjectId, AcademicSubjectRevision revision, int rowOrder, String choiceGroup) {
        return new AcademicCurriculumEntry(subjectId, revision, 1, "Básico", "Disciplinar", choiceGroup, rowOrder);
    }

    private AcademicSubjectRevision subjectRevision(UUID subjectId, String name, String credits) {
        return new AcademicSubjectRevision(UUID.randomUUID(), subjectId, name, new BigDecimal(credits));
    }
}
