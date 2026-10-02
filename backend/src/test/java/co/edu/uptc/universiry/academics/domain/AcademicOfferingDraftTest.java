package co.edu.uptc.universiry.academics.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademicOfferingDraftTest {

    private static final UUID PERIOD_ID = UUID.randomUUID();
    private static final UUID CURRICULUM_ID = UUID.randomUUID();
    private static final UUID SUBJECT_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2027, 1, 15);
    private static final LocalDate END = LocalDate.of(2027, 3, 15);

    @Test
    void creates_a_version_one_draft_and_normalizes_its_section_code() {
        // Arrange
        UUID draftId = UUID.randomUUID();

        // Act
        AcademicOfferingDraft draft = AcademicOfferingDraft.create(
                draftId, PERIOD_ID, CURRICULUM_ID, SUBJECT_ID, "  g-02 ", START, END, 30);

        // Assert
        assertEquals(draftId, draft.id());
        assertEquals(PERIOD_ID, draft.periodId());
        assertEquals(CURRICULUM_ID, draft.curriculumId());
        assertEquals(SUBJECT_ID, draft.subjectId());
        assertEquals("G-02", draft.sectionCode());
        assertEquals(START, draft.startsOn());
        assertEquals(END, draft.endsOn());
        assertEquals(30, draft.proposedCapacity());
        assertEquals(1, draft.version());
    }

    @Test
    void rejects_a_missing_identity_or_curriculum_entry() {
        // Arrange
        UUID draftId = UUID.randomUUID();

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> AcademicOfferingDraft.create(
                null, PERIOD_ID, CURRICULUM_ID, SUBJECT_ID, "G-02", START, END, 30));
        assertThrows(IllegalArgumentException.class, () -> AcademicOfferingDraft.create(
                draftId, PERIOD_ID, CURRICULUM_ID, null, "G-02", START, END, 30));
    }

    @Test
    void rejects_an_invalid_date_range_or_non_positive_capacity() {
        // Arrange
        UUID draftId = UUID.randomUUID();

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> AcademicOfferingDraft.create(
                draftId, PERIOD_ID, CURRICULUM_ID, SUBJECT_ID, "G-02", END, START, 30));
        assertThrows(IllegalArgumentException.class, () -> AcademicOfferingDraft.create(
                draftId, PERIOD_ID, CURRICULUM_ID, SUBJECT_ID, "G-02", START, END, 0));
    }

    @Test
    void revisions_require_the_observed_version_and_increment_it_once() {
        // Arrange
        AcademicOfferingDraft draft = AcademicOfferingDraft.create(
                UUID.randomUUID(), PERIOD_ID, CURRICULUM_ID, SUBJECT_ID, "G-02", START, END, 30);

        // Act
        AcademicOfferingDraft revised = draft.revise(1, "g-03", START.plusDays(1), END, 24);

        // Assert
        assertEquals("G-03", revised.sectionCode());
        assertEquals(START.plusDays(1), revised.startsOn());
        assertEquals(24, revised.proposedCapacity());
        assertEquals(2, revised.version());
        assertEquals(draft.id(), revised.id());
    }

    @Test
    void stale_revision_and_invalid_section_codes_are_rejected() {
        // Arrange
        AcademicOfferingDraft draft = AcademicOfferingDraft.create(
                UUID.randomUUID(), PERIOD_ID, CURRICULUM_ID, SUBJECT_ID, "G-02", START, END, 30);

        // Act + Assert
        assertThrows(AcademicOfferingVersionConflictException.class,
                () -> draft.revise(2, "G-03", START, END, 24));
        assertThrows(IllegalArgumentException.class,
                () -> draft.revise(1, "BAD CODE", START, END, 24));
    }
}
