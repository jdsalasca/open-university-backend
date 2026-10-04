package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;
import co.edu.uptc.universiry.academics.domain.AcademicCurriculumStatus;
import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultCurriculumPublicationServiceTest {

    private static final CurriculumProgramIdentity PROGRAM_IDENTITY = new CurriculumProgramIdentity(
            "PRE-001", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "TUNJA");
    private static final UUID CURRICULUM_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final String SOURCE_HASH = "a".repeat(64);

    @Test
    void previews_against_the_latest_published_curriculum_for_the_exact_program_identity() {
        // Arrange
        AcademicCatalogRepository repository = mock(AcademicCatalogRepository.class);
        AcademicCurriculumDetails publishedReference = publishedCurriculum();
        when(repository.findLatestPublishedCurriculum(PROGRAM_IDENTITY))
                .thenReturn(Optional.of(publishedReference));
        CurriculumPublicationService service = service(repository);

        // Act
        CurriculumImportPreview preview = service.previewCsv(
                new ByteArrayInputStream(new byte[0]), "academic.operator");

        // Assert
        assertEquals(1, preview.entryCount());
        assertEquals(CurriculumVersionComparison.Status.COMPARED, preview.comparison().status());
        assertEquals(CURRICULUM_ID, preview.comparison().reference().curriculumId());
        assertEquals(new CurriculumVersionComparison.Counts(0, 0, 0, 1), preview.comparison().counts());
        verify(repository).findLatestPublishedCurriculum(PROGRAM_IDENTITY);
    }

    @Test
    void returns_an_explicit_no_reference_result_when_the_program_has_no_published_curriculum() {
        // Arrange
        AcademicCatalogRepository repository = mock(AcademicCatalogRepository.class);
        when(repository.findLatestPublishedCurriculum(PROGRAM_IDENTITY)).thenReturn(Optional.empty());
        CurriculumPublicationService service = service(repository);

        // Act
        CurriculumImportPreview preview = service.previewCsv(
                new ByteArrayInputStream(new byte[0]), "academic.operator");

        // Assert
        assertEquals(CurriculumVersionComparison.Status.NO_REFERENCE, preview.comparison().status());
        assertNull(preview.comparison().reference());
        assertNull(preview.comparison().counts());
        assertTrue(preview.comparison().addedSamples().isEmpty());
        assertTrue(preview.comparison().removedSamples().isEmpty());
        assertTrue(preview.comparison().modifiedSamples().isEmpty());
        assertTrue(preview.comparison().unchangedSamples().isEmpty());
        verify(repository).findLatestPublishedCurriculum(PROGRAM_IDENTITY);
    }

    private static CurriculumPublicationService service(AcademicCatalogRepository repository) {
        CurriculumCsvParser parser = ignored -> new ParsedCurriculum(
                List.of(new ParsedCurriculumRow(2, csvValues())), SOURCE_HASH);
        CurriculumImportService importer = new CurriculumImportService(new CurriculumImportLimits(
                AcademicCatalogLimits.MAX_IMPORT_BYTES, AcademicCatalogLimits.MAX_IMPORT_ROWS));
        return new DefaultCurriculumPublicationService(parser, importer, repository);
    }

    private static Map<String, String> csvValues() {
        return Map.ofEntries(
                Map.entry("program_code", "PRE-001"),
                Map.entry("academic_level", "PREGRADO"),
                Map.entry("study_modality", "PRESENCIAL"),
                Map.entry("snies_code", "12345"),
                Map.entry("program_name", "Ingeniería de prueba"),
                Map.entry("faculty", "Facultad de prueba"),
                Map.entry("campus_code", "TUNJA"),
                Map.entry("campus_name", "Tunja"),
                Map.entry("curriculum_version", "2026-V1"),
                Map.entry("cohort_from", "2026-1"),
                Map.entry("cohort_through", ""),
                Map.entry("approval_reference", "Referencia de prueba"),
                Map.entry("semester", "1"),
                Map.entry("subject_code", "SUB-1"),
                Map.entry("subject_name", "Álgebra"),
                Map.entry("credits", "3.0"),
                Map.entry("formation_space", "Básico"),
                Map.entry("component", "Fundamentación"),
                Map.entry("choice_group", ""));
    }

    private static AcademicCurriculumDetails publishedCurriculum() {
        Instant publishedAt = Instant.parse("2025-01-15T12:00:00Z");
        CurriculumSummary summary = new CurriculumSummary(CURRICULUM_ID,
                UUID.fromString("00000000-0000-4000-8000-000000000002"),
                "PRE-001", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL, "TUNJA",
                "Ingeniería de prueba", "Facultad de prueba", "Tunja", "2025-V1", "2025-1", null,
                "Referencia anterior", AcademicCurriculumStatus.PUBLISHED, 1, SOURCE_HASH,
                publishedAt.minusSeconds(60), "operator", "operator", publishedAt);
        AcademicCurriculumEntrySummary entry = new AcademicCurriculumEntrySummary(
                UUID.fromString("00000000-0000-4000-8000-000000000003"),
                UUID.fromString("00000000-0000-4000-8000-000000000004"),
                "SUB-1", "Álgebra", new BigDecimal("3.00"), 1, "Básico", "Fundamentación", null, 1);
        return new AcademicCurriculumDetails(summary, List.of(entry));
    }
}
