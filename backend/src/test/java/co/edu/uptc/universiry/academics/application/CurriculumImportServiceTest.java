package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;
import co.edu.uptc.universiry.academics.domain.AcademicLevel;
import co.edu.uptc.universiry.academics.domain.StudyModality;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurriculumImportServiceTest {

    private final CurriculumImportService service = new CurriculumImportService(defaultLimits());

    @Test
    void validates_a_multi_semester_plan_and_normalizes_only_stable_codes() {
        // Arrange
        ParsedCurriculum parsed = parsed(
                row(2, values()),
                row(3, with(values(), "semester", "2", "subject_code", " mat-002 ",
                        "subject_name", "Álgebra Lineal", "credits", "4.00")));

        // Act
        ValidatedCurriculum validated = service.validate(parsed);

        // Assert
        assertEquals("ING-01", validated.program().programCode());
        assertEquals(AcademicLevel.PREGRADO, validated.program().academicLevel());
        assertEquals(StudyModality.PRESENCIAL, validated.program().studyModality());
        assertEquals("Ingeniería Ambiental", validated.program().programName());
        assertEquals("2026-1", validated.cohortFrom());
        assertEquals(2, validated.entries().size());
        assertEquals("CALC-001", validated.entries().getFirst().subjectCode());
        assertEquals("MAT-002", validated.entries().getLast().subjectCode());
        assertEquals("Álgebra Lineal", validated.entries().getLast().subjectName());
        assertEquals(new BigDecimal("4"), validated.entries().getLast().credits());
        assertEquals(64, validated.sourceSha256().length());
    }

    @Test
    void rejects_an_unsupported_academic_level_without_echoing_the_input() {
        // Arrange
        String privateValue = "POSGRADO-SECRET";
        ParsedCurriculum parsed = parsed(row(2, with(values(), "academic_level", privateValue)));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> service.validate(parsed));

        // Assert
        assertEquals(CurriculumCsvIssue.Code.UNSUPPORTED_ACADEMIC_LEVEL, error.issues().getFirst().code());
        assertEquals("academic_level", error.issues().getFirst().column());
        assertFalse(error.getMessage().contains(privateValue));
    }

    @Test
    void rejects_an_unsupported_study_modality() {
        // Arrange
        ParsedCurriculum parsed = parsed(row(2, with(values(), "study_modality", "DISTANCIA")));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> service.validate(parsed));

        // Assert
        assertEquals(CurriculumCsvIssue.Code.UNSUPPORTED_STUDY_MODALITY, error.issues().getFirst().code());
        assertEquals("study_modality", error.issues().getFirst().column());
    }

    @Test
    void rejects_metadata_that_changes_between_rows_including_level_modality_and_campus() {
        // Arrange
        ParsedCurriculum parsed = parsed(
                row(2, values()),
                row(3, with(values(), "academic_level", "POSGRADO", "study_modality", "DISTANCIA",
                        "campus_code", "DUITAMA")));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> service.validate(parsed));

        // Assert
        assertTrue(error.issues().stream().anyMatch(issue ->
                issue.rowNumber() == 3 && issue.column().equals("academic_level")
                        && issue.code() == CurriculumCsvIssue.Code.INCONSISTENT_METADATA));
        assertTrue(error.issues().stream().anyMatch(issue ->
                issue.rowNumber() == 3 && issue.column().equals("study_modality")
                        && issue.code() == CurriculumCsvIssue.Code.INCONSISTENT_METADATA));
        assertTrue(error.issues().stream().anyMatch(issue ->
                issue.rowNumber() == 3 && issue.column().equals("campus_code")
                        && issue.code() == CurriculumCsvIssue.Code.INCONSISTENT_METADATA));
    }

    @Test
    void rejects_blank_required_fields_and_reports_only_the_column() {
        // Arrange
        ParsedCurriculum parsed = parsed(row(2, with(values(), "subject_name", "   ")));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> service.validate(parsed));

        // Assert
        assertEquals(CurriculumCsvIssue.Code.REQUIRED_VALUE_MISSING, error.issues().getFirst().code());
        assertEquals("subject_name", error.issues().getFirst().column());
    }

    @Test
    void rejects_overlong_fields_without_returning_the_cell_value() {
        // Arrange
        String secretText = "PRIVATE-" + "x".repeat(AcademicCatalogLimits.MAX_PROGRAM_NAME_LENGTH);
        ParsedCurriculum parsed = parsed(row(2, with(values(), "program_name", secretText)));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> service.validate(parsed));

        // Assert
        assertTrue(error.issues().stream().anyMatch(issue ->
                "program_name".equals(issue.column()) && issue.code() == CurriculumCsvIssue.Code.FIELD_TOO_LONG));
        assertFalse(error.getMessage().contains(secretText));
    }

    @Test
    void rejects_nonpositive_overprecise_and_malformed_credit_values() {
        // Arrange
        List<String> invalidCredits = List.of("0", "-1", "1.234", "1000", "1e2", "three");

        // Act + Assert
        for (String credits : invalidCredits) {
            CurriculumCsvException error = assertThrows(
                    CurriculumCsvException.class,
                    () -> service.validate(parsed(row(2, with(values(), "credits", credits)))));
            assertEquals(CurriculumCsvIssue.Code.INVALID_CREDITS, error.issues().getFirst().code());
        }
    }

    @Test
    void rejects_semesters_outside_the_supported_positive_integer_range() {
        // Arrange
        List<String> invalidSemesters = List.of("0", "32768", "1.5", "-1", "1e1");

        // Act + Assert
        for (String semester : invalidSemesters) {
            CurriculumCsvException error = assertThrows(
                    CurriculumCsvException.class,
                    () -> service.validate(parsed(row(2, with(values(), "semester", semester)))));
            assertEquals(CurriculumCsvIssue.Code.INVALID_SEMESTER, error.issues().getFirst().code());
        }
    }

    @Test
    void rejects_duplicate_subject_codes_after_case_and_whitespace_normalization() {
        // Arrange
        ParsedCurriculum parsed = parsed(
                row(2, values()),
                row(3, with(values(), "subject_code", " calc-001 ", "semester", "2")));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> service.validate(parsed));

        // Assert
        assertTrue(error.issues().stream().anyMatch(issue ->
                issue.rowNumber() == 3 && "subject_code".equals(issue.column())
                        && issue.code() == CurriculumCsvIssue.Code.DUPLICATE_SUBJECT_CODE));
    }

    @Test
    void rejects_a_cohort_end_that_precedes_the_start() {
        // Arrange
        ParsedCurriculum parsed = parsed(row(2, with(values(), "cohort_from", "2026-2", "cohort_through", "2026-1")));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> service.validate(parsed));

        // Assert
        assertTrue(error.issues().stream().anyMatch(issue ->
                "cohort_through".equals(issue.column())
                        && issue.code() == CurriculumCsvIssue.Code.INVALID_COHORT_RANGE));
    }

    @Test
    void permits_blank_optional_snies_cohort_end_and_choice_group() {
        // Arrange
        ParsedCurriculum parsed = parsed(row(2, with(values(), "snies_code", "", "cohort_through", "",
                "choice_group", "")));

        // Act
        ValidatedCurriculum validated = service.validate(parsed);

        // Assert
        assertEquals("Ingeniería Ambiental", validated.program().programName());
        assertEquals(null, validated.program().sniesCode());
        assertEquals(null, validated.cohortThrough());
        assertEquals(null, validated.entries().getFirst().choiceGroup());
    }

    @Test
    void enforces_the_configured_row_bound_before_producing_a_validated_plan() {
        // Arrange
        CurriculumImportService limitedService = new CurriculumImportService(new CurriculumImportLimits(1024, 1));
        ParsedCurriculum parsed = parsed(row(2, values()), row(3, with(values(), "subject_code", "MAT-002")));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> limitedService.validate(parsed));

        // Assert
        assertEquals(CurriculumCsvException.Code.TOO_MANY_ROWS, error.code());
    }

    @Test
    void rejects_configured_import_bounds_above_the_catalog_hard_limits() {
        // Arrange
        int oversizedByteLimit = AcademicCatalogLimits.MAX_IMPORT_BYTES + 1;
        int oversizedRowLimit = AcademicCatalogLimits.MAX_IMPORT_ROWS + 1;

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new CurriculumImportLimits(oversizedByteLimit, 1));
        assertThrows(IllegalArgumentException.class, () -> new CurriculumImportLimits(1024, oversizedRowLimit));
    }

    @Test
    void parsed_curriculum_owns_an_immutable_snapshot_of_rows() {
        // Arrange
        ArrayList<ParsedCurriculumRow> sourceRows = new ArrayList<>();
        sourceRows.add(row(2, values()));
        ParsedCurriculum parsed = new ParsedCurriculum(sourceRows, "a".repeat(64));

        // Act
        sourceRows.clear();

        // Assert
        assertEquals(1, parsed.rows().size());
        assertThrows(UnsupportedOperationException.class, () -> parsed.rows().clear());
    }

    @Test
    void parsed_row_owns_an_immutable_snapshot_of_cell_values() {
        // Arrange
        Map<String, String> sourceValues = new HashMap<>();
        sourceValues.put("program_code", "ING-01");
        ParsedCurriculumRow row = new ParsedCurriculumRow(2, sourceValues);

        // Act
        sourceValues.put("program_code", "CHANGED");

        // Assert
        assertEquals("ING-01", row.values().get("program_code"));
        assertThrows(UnsupportedOperationException.class, () -> row.values().put("program_code", "OTHER"));
    }

    @Test
    void validated_curriculum_owns_an_immutable_snapshot_of_entries() {
        // Arrange
        ValidatedCurriculumEntry entry = new ValidatedCurriculumEntry(
                2, 1, 1, "CALC-001", "Cálculo", new java.math.BigDecimal("3"),
                "Disciplinar", "Obligatorio", null);
        ArrayList<ValidatedCurriculumEntry> sourceEntries = new ArrayList<>(List.of(entry));
        ValidatedCurriculum validated = new ValidatedCurriculum(
                new ValidatedProgram("ING-01", AcademicLevel.PREGRADO, StudyModality.PRESENCIAL,
                        null, "Ingeniería Ambiental", "Ingeniería", "TUNJA", "Tunja"),
                "2026-v1", "2026-1", null, "Acuerdo 01", sourceEntries, "a".repeat(64));

        // Act
        sourceEntries.clear();

        // Assert
        assertEquals(1, validated.entries().size());
        assertThrows(UnsupportedOperationException.class, () -> validated.entries().clear());
    }

    private static CurriculumImportLimits defaultLimits() {
        return new CurriculumImportLimits(
                AcademicCatalogLimits.MAX_IMPORT_BYTES,
                AcademicCatalogLimits.MAX_IMPORT_ROWS);
    }

    private static ParsedCurriculum parsed(ParsedCurriculumRow... rows) {
        return new ParsedCurriculum(List.of(rows), "a".repeat(64));
    }

    private static ParsedCurriculumRow row(int rowNumber, Map<String, String> values) {
        return new ParsedCurriculumRow(rowNumber, values);
    }

    private static Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("program_code", " ing-01 ");
        values.put("academic_level", " pregrado ");
        values.put("study_modality", " presencial ");
        values.put("snies_code", "12345");
        values.put("program_name", "Ingeniería Ambiental");
        values.put("faculty", "Ingeniería");
        values.put("campus_code", " tunja ");
        values.put("campus_name", "Tunja");
        values.put("curriculum_version", "2026-v1");
        values.put("cohort_from", "2026-1");
        values.put("cohort_through", "2028-2");
        values.put("approval_reference", "Acuerdo 01 de 2026");
        values.put("semester", "1");
        values.put("subject_code", " calc-001 ");
        values.put("subject_name", "Cálculo Diferencial");
        values.put("credits", "3.00");
        values.put("formation_space", "Disciplinar");
        values.put("component", "Obligatorio");
        values.put("choice_group", "");
        return values;
    }

    private static Map<String, String> with(Map<String, String> original, String... overrides) {
        Map<String, String> values = new HashMap<>(original);
        for (int index = 0; index < overrides.length; index += 2) {
            values.put(overrides[index], overrides[index + 1]);
        }
        return values;
    }
}
