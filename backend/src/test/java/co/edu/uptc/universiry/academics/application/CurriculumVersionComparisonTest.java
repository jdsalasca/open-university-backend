package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCurriculumStatus;
import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurriculumVersionComparisonTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void compares_normalized_codes_and_counts_added_removed_modified_and_unchanged_rows() {
        // Arrange
        ValidatedCurriculum incoming = incoming(List.of(
                incomingEntry("same", "Álgebra", "3.0", 1, 1),
                incomingEntry("SUB-ADD", "Biología", "4", 2, 2),
                incomingEntry("SUB-CHANGE", "Física", "2", 4, 3)));
        AcademicCurriculumDetails reference = reference(List.of(
                referenceEntry("SAME", "Álgebra", "3.00", 1, 1),
                referenceEntry("sub-remove", "Química", "4", 2, 2),
                referenceEntry("sub-change", "Física", "2", 3, 3)));

        // Act
        CurriculumVersionComparison comparison = CurriculumVersionComparison.compare(incoming, reference);

        // Assert
        assertEquals(CurriculumVersionComparison.Status.COMPARED, comparison.status());
        assertEquals(new CurriculumVersionComparison.Counts(1, 1, 1, 1), comparison.counts());
        assertEquals("SUB-ADD", comparison.addedSamples().getFirst().subjectCode());
        assertEquals("SUB-REMOVE", comparison.removedSamples().getFirst().subjectCode());
        assertEquals("SUB-CHANGE", comparison.modifiedSamples().getFirst().subjectCode());
        assertEquals("SAME", comparison.unchangedSamples().getFirst().subjectCode());
        assertEquals(incoming.entries().size(), comparison.counts().added()
                + comparison.counts().modified() + comparison.counts().unchanged());
        assertEquals(reference.entries().size(), comparison.counts().removed()
                + comparison.counts().modified() + comparison.counts().unchanged());
    }

    @Test
    void marks_each_changed_field_and_compares_credits_without_decimal_scale() {
        // Arrange
        ValidatedCurriculum incoming = incoming(List.of(
                new ValidatedCurriculumEntry(2, 1, 2, "SUB-ALL", "Nombre nuevo", new BigDecimal("4.50"),
                        "Formación nueva", "Componente nuevo", "Opción nueva"),
                incomingEntry("SUB-SCALE", "Misma asignatura", "3.0", 1, 2)));
        AcademicCurriculumDetails reference = reference(List.of(
                referenceEntry("SUB-ALL", "Nombre anterior", "3.5", 1, 4),
                referenceEntry("SUB-SCALE", "Misma asignatura", "3.00", 1, 2)));

        // Act
        CurriculumVersionComparison comparison = CurriculumVersionComparison.compare(incoming, reference);

        // Assert
        assertEquals(new CurriculumVersionComparison.Counts(0, 0, 1, 1), comparison.counts());
        assertEquals(List.of(CurriculumVersionComparison.ChangedField.NAME,
                        CurriculumVersionComparison.ChangedField.CREDITS,
                        CurriculumVersionComparison.ChangedField.SEMESTER,
                        CurriculumVersionComparison.ChangedField.ORDER,
                        CurriculumVersionComparison.ChangedField.FORMATION_SPACE,
                        CurriculumVersionComparison.ChangedField.COMPONENT,
                        CurriculumVersionComparison.ChangedField.CHOICE_GROUP),
                comparison.modifiedSamples().getFirst().changedFields());
        assertEquals("SUB-SCALE", comparison.unchangedSamples().getFirst().subjectCode());
    }

    @Test
    void keeps_full_counts_and_caps_each_category_sample_at_ten() {
        // Arrange
        List<ValidatedCurriculumEntry> incomingEntries = new ArrayList<>();
        List<AcademicCurriculumEntrySummary> referenceEntries = new ArrayList<>();
        for (int index = 1; index <= 12; index++) {
            incomingEntries.add(incomingEntry("SUB-ADD-%02d".formatted(index), "Nueva", "1", 1, index));
            incomingEntries.add(incomingEntry("SUB-MOD-%02d".formatted(index), "Nueva", "2", 2, index + 12));
            incomingEntries.add(incomingEntry("SUB-SAME-%02d".formatted(index), "Igual", "3", 3, index + 24));
            referenceEntries.add(referenceEntry("SUB-MOD-%02d".formatted(index), "Anterior", "2", 2, index + 12));
            referenceEntries.add(referenceEntry("SUB-SAME-%02d".formatted(index), "Igual", "3.00", 3, index + 24));
            referenceEntries.add(referenceEntry("SUB-REM-%02d".formatted(index), "Retirada", "4", 4, index + 36));
        }

        // Act
        CurriculumVersionComparison comparison = CurriculumVersionComparison.compare(
                incoming(incomingEntries), reference(referenceEntries));

        // Assert
        assertEquals(new CurriculumVersionComparison.Counts(12, 12, 12, 12), comparison.counts());
        assertEquals(10, comparison.addedSamples().size());
        assertEquals(10, comparison.removedSamples().size());
        assertEquals(10, comparison.modifiedSamples().size());
        assertEquals(10, comparison.unchangedSamples().size());
    }

    @Test
    void represents_missing_reference_without_comparison_counts() {
        // Arrange + Act
        CurriculumVersionComparison comparison = CurriculumVersionComparison.noReference();

        // Assert
        assertEquals(CurriculumVersionComparison.Status.NO_REFERENCE, comparison.status());
        assertNull(comparison.reference());
        assertNull(comparison.counts());
        assertEquals(List.of(), comparison.addedSamples());
        assertEquals(List.of(), comparison.removedSamples());
        assertEquals(List.of(), comparison.modifiedSamples());
        assertEquals(List.of(), comparison.unchangedSamples());
    }

    @Test
    void rejects_a_reference_that_is_not_published() {
        // Arrange
        ValidatedCurriculum incoming = incoming(List.of(incomingEntry("SUB-1", "Álgebra", "3", 1, 1)));
        AcademicCurriculumDetails draft = reference(
                "PRG-TEST", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "TUNJA",
                AcademicCurriculumStatus.DRAFT, 1, null,
                List.of(referenceEntry("SUB-1", "Álgebra", "3", 1, 1)));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> CurriculumVersionComparison.compare(incoming, draft));
    }

    @Test
    void rejects_a_published_reference_without_its_publication_timestamp() {
        // Arrange
        ValidatedCurriculum incoming = incoming(List.of(incomingEntry("SUB-1", "Álgebra", "3", 1, 1)));
        AcademicCurriculumDetails missingTimestamp = reference(
                "PRG-TEST", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "TUNJA",
                AcademicCurriculumStatus.PUBLISHED, 1, null,
                List.of(referenceEntry("SUB-1", "Álgebra", "3", 1, 1)));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> CurriculumVersionComparison.compare(incoming, missingTimestamp));
    }

    @Test
    void rejects_a_reference_for_another_program_identity() {
        // Arrange
        ValidatedCurriculum incoming = incoming(List.of(incomingEntry("SUB-1", "Álgebra", "3", 1, 1)));
        AcademicCurriculumDetails otherCampus = reference(
                "PRG-TEST", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "SOGAMOSO",
                AcademicCurriculumStatus.PUBLISHED, 1, PUBLISHED_AT,
                List.of(referenceEntry("SUB-1", "Álgebra", "3", 1, 1)));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> CurriculumVersionComparison.compare(incoming, otherCampus));
    }

    @Test
    void rejects_a_reference_with_another_program_code() {
        // Arrange
        ValidatedCurriculum incoming = incoming(List.of(incomingEntry("SUB-1", "Álgebra", "3", 1, 1)));
        AcademicCurriculumDetails otherProgram = reference(
                "PRG-OTHER", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "TUNJA",
                AcademicCurriculumStatus.PUBLISHED, 1, PUBLISHED_AT,
                List.of(referenceEntry("SUB-1", "Álgebra", "3", 1, 1)));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> CurriculumVersionComparison.compare(incoming, otherProgram));
    }

    @Test
    void rejects_a_reference_when_its_declared_entry_count_does_not_match_its_detail() {
        // Arrange
        ValidatedCurriculum incoming = incoming(List.of(incomingEntry("SUB-1", "Álgebra", "3", 1, 1)));
        AcademicCurriculumDetails inconsistent = reference(
                "PRG-TEST", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "TUNJA",
                AcademicCurriculumStatus.PUBLISHED, 2, PUBLISHED_AT,
                List.of(referenceEntry("SUB-1", "Álgebra", "3", 1, 1)));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> CurriculumVersionComparison.compare(incoming, inconsistent));
    }

    @Test
    void rejects_duplicate_subject_codes_after_normalization_on_either_side() {
        // Arrange
        ValidatedCurriculum duplicateIncoming = incoming(List.of(
                incomingEntry("SUB-1", "Álgebra", "3", 1, 1),
                incomingEntry(" sub-1 ", "Álgebra", "3", 1, 2)));
        AcademicCurriculumDetails singleReference = reference(List.of(
                referenceEntry("SUB-1", "Álgebra", "3", 1, 1)));
        ValidatedCurriculum singleIncoming = incoming(List.of(
                incomingEntry("SUB-1", "Álgebra", "3", 1, 1)));
        AcademicCurriculumDetails duplicateReference = reference(List.of(
                referenceEntry("SUB-1", "Álgebra", "3", 1, 1),
                referenceEntry(" sub-1 ", "Álgebra", "3", 1, 2)));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> CurriculumVersionComparison.compare(duplicateIncoming, singleReference));
        assertThrows(IllegalArgumentException.class,
                () -> CurriculumVersionComparison.compare(singleIncoming, duplicateReference));
    }

    private static ValidatedCurriculum incoming(List<ValidatedCurriculumEntry> entries) {
        return new ValidatedCurriculum(new ValidatedProgram("PRG-TEST", AcademicLevel.PREGRADO,
                StudyModality.PRESENCIAL, "12345", "Programa de prueba", "Facultad sintética", "TUNJA", "Tunja"),
                "V2", "2026-1", "2028-2", "Referencia sintética", entries, "a".repeat(64));
    }

    private static AcademicCurriculumDetails reference(List<AcademicCurriculumEntrySummary> entries) {
        return reference("PRG-TEST", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "TUNJA",
                AcademicCurriculumStatus.PUBLISHED, entries.size(), PUBLISHED_AT, entries);
    }

    private static AcademicCurriculumDetails reference(
            String programCode,
            AcademicLevel level,
            StudyModality modality,
            String campusCode,
            AcademicCurriculumStatus status,
            int declaredEntryCount,
            Instant publishedAt,
            List<AcademicCurriculumEntrySummary> entries
    ) {
        CurriculumSummary summary = new CurriculumSummary(UUID.randomUUID(), UUID.randomUUID(), programCode,
                level, modality, campusCode, "Programa de prueba",
                "Facultad sintética", "Tunja", "V1", "2026-1", "2028-2", "Referencia sintética",
                status, declaredEntryCount, "b".repeat(64), PUBLISHED_AT, "actor",
                status == AcademicCurriculumStatus.PUBLISHED ? "actor" : null, publishedAt);
        return new AcademicCurriculumDetails(summary, entries);
    }

    private static ValidatedCurriculumEntry incomingEntry(
            String code, String name, String credits, int semester, int rowOrder
    ) {
        return new ValidatedCurriculumEntry(rowOrder + 1, rowOrder, semester, code, name,
                new BigDecimal(credits), "Formación", "Obligatorio", null);
    }

    private static AcademicCurriculumEntrySummary referenceEntry(
            String code, String name, String credits, int semester, int rowOrder
    ) {
        return new AcademicCurriculumEntrySummary(UUID.randomUUID(), UUID.randomUUID(), code, name,
                new BigDecimal(credits), semester, "Formación", "Obligatorio", null, rowOrder);
    }
}
