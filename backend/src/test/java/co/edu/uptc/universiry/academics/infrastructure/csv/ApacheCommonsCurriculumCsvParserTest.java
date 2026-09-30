package co.edu.uptc.universiry.academics.infrastructure.csv;

import co.edu.uptc.universiry.academics.application.CurriculumCsvException;
import co.edu.uptc.universiry.academics.application.CurriculumCsvIssue;
import co.edu.uptc.universiry.academics.application.CurriculumCsvSchema;
import co.edu.uptc.universiry.academics.application.CurriculumImportLimits;
import co.edu.uptc.universiry.academics.application.ParsedCurriculum;
import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApacheCommonsCurriculumCsvParserTest {

    private final ApacheCommonsCurriculumCsvParser parser = new ApacheCommonsCurriculumCsvParser(
            new CurriculumImportLimits(
                    AcademicCatalogLimits.MAX_IMPORT_BYTES,
                    AcademicCatalogLimits.MAX_IMPORT_ROWS));

    @Test
    void parses_utf8_bom_quoted_commas_and_accents_without_changing_values() {
        // Arrange
        String csv = "\uFEFF" + String.join(",", CurriculumCsvSchema.HEADERS)
                + "\n" + row("Ingeniería, Ambiental", "Sistemas de Información");

        // Act
        ParsedCurriculum parsed = parser.parse(stream(csv));

        // Assert
        assertEquals(1, parsed.rows().size());
        assertEquals(2, parsed.rows().getFirst().rowNumber());
        assertEquals("Ingeniería, Ambiental", parsed.rows().getFirst().values().get("program_name"));
        assertEquals("Sistemas de Información", parsed.rows().getFirst().values().get("subject_name"));
        assertTrue(parsed.sourceSha256().matches("[0-9a-f]{64}"));
    }

    @Test
    void preserves_all_rows_in_source_order_for_a_multi_semester_plan() {
        // Arrange
        String csv = String.join(",", CurriculumCsvSchema.HEADERS)
                + "\n" + row("Programa", "Cálculo I")
                + "\n" + row("Programa", "Física I", "2");

        // Act
        ParsedCurriculum parsed = parser.parse(stream(csv));

        // Assert
        assertEquals(List.of(2, 3), parsed.rows().stream().map(row -> row.rowNumber()).toList());
        assertEquals("1", parsed.rows().getFirst().values().get("semester"));
        assertEquals("2", parsed.rows().getLast().values().get("semester"));
    }

    @Test
    void rejects_missing_headers_with_a_safe_contract_error() {
        // Arrange
        String header = String.join(",", CurriculumCsvSchema.HEADERS.subList(0, 18));

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> parser.parse(stream(header + "\n")));

        // Assert
        assertEquals(CurriculumCsvException.Code.INVALID_HEADERS, error.code());
    }

    @Test
    void rejects_unknown_headers_with_a_safe_contract_error() {
        // Arrange
        String header = String.join(",", CurriculumCsvSchema.HEADERS) + ",private_payload";

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> parser.parse(stream(header + "\n")));

        // Assert
        assertEquals(CurriculumCsvException.Code.INVALID_HEADERS, error.code());
        assertEquals("INVALID_HEADERS", error.getMessage());
    }

    @Test
    void rejects_duplicate_headers_without_echoing_header_text() {
        // Arrange
        String duplicate = String.join(",", CurriculumCsvSchema.HEADERS).replace(
                "program_name", "program_code");

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> parser.parse(stream(duplicate + "\n")));

        // Assert
        assertEquals(CurriculumCsvException.Code.INVALID_HEADERS, error.code());
        assertTrue(error.getMessage().contains("INVALID_HEADERS"));
    }

    @Test
    void rejects_malformed_utf8_instead_of_replacing_invalid_bytes() {
        // Arrange
        byte[] header = (String.join(",", CurriculumCsvSchema.HEADERS) + "\n").getBytes(StandardCharsets.UTF_8);
        byte[] bytes = Arrays.copyOf(header, header.length + 2);
        bytes[header.length] = (byte) 0xC3;
        bytes[header.length + 1] = (byte) 0x28;

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class,
                () -> parser.parse(new ByteArrayInputStream(bytes)));

        // Assert
        assertEquals(CurriculumCsvException.Code.INVALID_UTF8, error.code());
    }

    @Test
    void rejects_an_empty_stream() {
        // Arrange
        ByteArrayInputStream empty = new ByteArrayInputStream(new byte[0]);

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> parser.parse(empty));

        // Assert
        assertEquals(CurriculumCsvException.Code.EMPTY_FILE, error.code());
    }

    @Test
    void rejects_an_unclosed_quoted_field_as_malformed_csv() {
        // Arrange
        String csv = String.join(",", CurriculumCsvSchema.HEADERS) + "\n\"unfinished";

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> parser.parse(stream(csv)));

        // Assert
        assertEquals(CurriculumCsvException.Code.MALFORMED_CSV, error.code());
    }

    @Test
    void rejects_a_data_row_with_the_wrong_number_of_fields() {
        // Arrange
        String csv = String.join(",", CurriculumCsvSchema.HEADERS) + "\nonly-one-field";

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> parser.parse(stream(csv)));

        // Assert
        assertEquals(CurriculumCsvException.Code.INVALID_DATA, error.code());
        assertEquals(2, error.issues().getFirst().rowNumber());
        assertEquals(CurriculumCsvIssue.Code.INVALID_ROW_WIDTH, error.issues().getFirst().code());
    }

    @Test
    void rejects_actual_stream_bytes_over_the_limit_without_a_trusted_size_argument() {
        // Arrange
        byte[] oversized = new byte[AcademicCatalogLimits.MAX_IMPORT_BYTES + 1];
        Arrays.fill(oversized, (byte) 'x');

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class,
                () -> parser.parse(new ByteArrayInputStream(oversized)));

        // Assert
        assertEquals(CurriculumCsvException.Code.FILE_TOO_LARGE, error.code());
    }

    @Test
    void rejects_more_than_ten_thousand_records_even_when_the_file_is_under_the_byte_limit() {
        // Arrange
        String header = String.join(",", CurriculumCsvSchema.HEADERS);
        String record = String.join(",", Collections.nCopies(CurriculumCsvSchema.HEADERS.size(), "x"));
        String csv = header + "\n" + String.join("\n", Collections.nCopies(
                AcademicCatalogLimits.MAX_IMPORT_ROWS + 1, record));
        assertTrue(csv.getBytes(StandardCharsets.UTF_8).length < AcademicCatalogLimits.MAX_IMPORT_BYTES);

        // Act
        CurriculumCsvException error = assertThrows(
                CurriculumCsvException.class, () -> parser.parse(stream(csv)));

        // Assert
        assertEquals(CurriculumCsvException.Code.TOO_MANY_ROWS, error.code());
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String row(String programName, String subjectName) {
        return row(programName, subjectName, "1");
    }

    private static String row(String programName, String subjectName, String semester) {
        List<String> values = List.of(
                "ING-01", "PREGRADO", "PRESENCIAL", "12345", programName, "Ingeniería",
                "TUNJA", "Tunja", "V1", "2026-1", "", "Acuerdo 01", semester, "CALC-01",
                subjectName, "3", "Disciplinar", "Obligatorio", "");
        return values.stream().map(ApacheCommonsCurriculumCsvParserTest::quoteIfNeeded)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String quoteIfNeeded(String value) {
        return value.contains(",") || value.contains("\"") || value.contains("\n")
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }
}
