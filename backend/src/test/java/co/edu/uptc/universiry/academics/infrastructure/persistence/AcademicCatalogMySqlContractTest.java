package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicCatalogQueryService;
import co.edu.uptc.universiry.academics.application.AcademicCurriculumEntriesPage;
import co.edu.uptc.universiry.academics.application.CurriculumEntriesPageQuery;
import co.edu.uptc.universiry.academics.application.CurriculumPublicationService;
import co.edu.uptc.universiry.academics.application.CurriculumSummary;
import co.edu.uptc.universiry.academics.application.CurriculumCsvSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "universiry.mysql-contract.enabled", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AcademicCatalogMySqlContractTest {

    private static final String ACTOR = "synthetic-mysql-contract-test";
    private static final String CSV_HEADER = String.join(",", CurriculumCsvSchema.HEADERS);
    private static final int PERFORMANCE_ROWS = 10_000;
    private static final int PERFORMANCE_WARMUPS = 10;
    private static final int PERFORMANCE_SAMPLES = 50;

    @Autowired
    private AcademicCatalogQueryService queryService;

    @Autowired
    private CurriculumPublicationService publicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID smallCurriculumId;
    private UUID performanceCurriculumId;

    @DynamicPropertySource
    static void mysqlDatasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_URL"));
        properties.add("spring.datasource.username", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_USERNAME"));
        properties.add("spring.datasource.password", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeEach
    void publish_synthetic_curriculum() {
        String programCode = "MYSQL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                .toUpperCase(Locale.ROOT);
        String csv = String.join("\r\n",
                CSV_HEADER,
                row(programCode, "V1", "2026-1", "MAT_001", "Cálculo 100%", "1"),
                row(programCode, "V1", "2026-1", "PHY-002", "Física Aplicada", "2"),
                row(programCode, "V1", "2026-1", "BIO-003", "Biología General", "3"),
                row(programCode, "V1", "2026-1", "QUI-004", "Óptica !", "4"),
                "");
        CurriculumSummary draft = publicationService.importCsv(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), ACTOR);

        publicationService.publish(draft.id(), ACTOR);
        smallCurriculumId = draft.id();
    }

    @AfterTransaction
    void rolls_back_synthetic_curricula_after_each_contract_test() {
        // Assert
        assertEquals(0, countCurricula(smallCurriculumId));
        if (performanceCurriculumId != null) {
            assertEquals(0, countCurricula(performanceCurriculumId));
        }
    }

    @Test
    void runs_against_mysql_84() {
        // Arrange
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);

        // Act
        boolean usesExpectedVersion = version != null && version.startsWith("8.4.");

        // Assert
        assertTrue(usesExpectedVersion, () -> "Expected MySQL 8.4 but connected to " + version);
    }

    @Test
    void mysql_search_is_accent_and_case_insensitive_and_keeps_like_wildcards_literal() {
        // Arrange
        CurriculumEntriesPageQuery accentSearch = new CurriculumEntriesPageQuery(1, 100, "calculo", null);
        CurriculumEntriesPageQuery percentSearch = new CurriculumEntriesPageQuery(1, 100, "%", null);
        CurriculumEntriesPageQuery underscoreSearch = new CurriculumEntriesPageQuery(1, 100, "_", null);
        CurriculumEntriesPageQuery escapeSearch = new CurriculumEntriesPageQuery(1, 100, "!", null);

        // Act
        AcademicCurriculumEntriesPage accentMatches = queryService.publishedCurriculumEntries(
                smallCurriculumId, accentSearch);
        AcademicCurriculumEntriesPage percentMatches = queryService.publishedCurriculumEntries(
                smallCurriculumId, percentSearch);
        AcademicCurriculumEntriesPage underscoreMatches = queryService.publishedCurriculumEntries(
                smallCurriculumId, underscoreSearch);
        AcademicCurriculumEntriesPage escapeMatches = queryService.publishedCurriculumEntries(
                smallCurriculumId, escapeSearch);

        // Assert
        assertEquals(1, accentMatches.totalItems());
        assertEquals("MAT_001", accentMatches.entries().getFirst().subjectCode());
        assertEquals(1, percentMatches.totalItems());
        assertEquals("MAT_001", percentMatches.entries().getFirst().subjectCode());
        assertEquals(1, underscoreMatches.totalItems());
        assertEquals("MAT_001", underscoreMatches.entries().getFirst().subjectCode());
        assertEquals(1, escapeMatches.totalItems());
        assertEquals("QUI-004", escapeMatches.entries().getFirst().subjectCode());
    }

    @Test
    void mysql_backfill_reconstructs_search_snapshots_for_existing_entries() {
        // Arrange
        String legacyEntries = "academic_entry_backfill_contract";
        jdbcTemplate.execute("""
                CREATE TEMPORARY TABLE academic_entry_backfill_contract (
                    subject_id CHAR(36) NOT NULL,
                    subject_revision_id CHAR(36) NOT NULL,
                    row_order INT NOT NULL,
                    search_subject_code VARCHAR(64) NOT NULL DEFAULT '',
                    search_subject_name VARCHAR(240) NOT NULL DEFAULT ''
                )
                """);
        try {
            jdbcTemplate.update("""
                    INSERT INTO academic_entry_backfill_contract
                        (subject_id, subject_revision_id, row_order)
                    SELECT subject_id, subject_revision_id, row_order
                    FROM academic_curriculum_entry
                    WHERE curriculum_id = ?
                    """, smallCurriculumId.toString());

            // Act
            jdbcTemplate.execute("""
                    UPDATE academic_entry_backfill_contract
                    SET search_subject_code = (
                            SELECT subject_code
                            FROM academic_subject
                            WHERE academic_subject.subject_id = academic_entry_backfill_contract.subject_id
                        ),
                        search_subject_name = (
                            SELECT subject_name
                            FROM academic_subject_revision
                            WHERE academic_subject_revision.subject_revision_id =
                                      academic_entry_backfill_contract.subject_revision_id
                              AND academic_subject_revision.subject_id = academic_entry_backfill_contract.subject_id
                        )
                    """);
            List<String> snapshots = jdbcTemplate.query(
                    "SELECT search_subject_code, search_subject_name "
                            + "FROM academic_entry_backfill_contract ORDER BY row_order",
                    (resultSet, rowNumber) -> resultSet.getString("search_subject_code") + "|"
                            + resultSet.getString("search_subject_name"));

            // Assert
            assertEquals(List.of(
                    "MAT_001|Cálculo 100%",
                    "PHY-002|Física Aplicada",
                    "BIO-003|Biología General",
                    "QUI-004|Óptica !"), snapshots);
        }
        finally {
            jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS " + legacyEntries);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "universiry.mysql-performance.enabled", matches = "true")
    void reports_pagination_latency_for_ten_thousand_synthetic_entries() {
        // Arrange
        UUID curriculumId = seedPerformanceCurriculum(PERFORMANCE_ROWS);
        performanceCurriculumId = curriculumId;
        CurriculumEntriesPageQuery pageQuery = new CurriculumEntriesPageQuery(1, 100, null, null);
        CurriculumEntriesPageQuery filteredQuery = new CurriculumEntriesPageQuery(1, 100, "09999", null);

        // Act
        List<Long> unfilteredNanos = sampleQuery(curriculumId, pageQuery);
        List<Long> filteredNanos = sampleQuery(curriculumId, filteredQuery);
        AcademicCurriculumEntriesPage firstPage = queryService.publishedCurriculumEntries(curriculumId, pageQuery);
        AcademicCurriculumEntriesPage matchingPage = queryService.publishedCurriculumEntries(curriculumId, filteredQuery);

        // Assert
        assertEquals(PERFORMANCE_ROWS, firstPage.totalItems());
        assertEquals(CurriculumEntriesPageQuery.MAX_PAGE_SIZE, firstPage.entries().size());
        assertEquals(1, matchingPage.totalItems());
        assertEquals("MAT09999", matchingPage.entries().getFirst().subjectCode());
        printQueryPlans(curriculumId);
        printMetrics("unfiltered-first-page", unfilteredNanos);
        printMetrics("substring-filter", filteredNanos);
        assertTrue(averageMillis(unfilteredNanos) < 50.0,
                () -> "Expected unfiltered MySQL pagination average below 50 ms for 10,000 rows; observed "
                        + String.format(Locale.ROOT, "%.3f ms", averageMillis(unfilteredNanos)));
        assertTrue(averageMillis(filteredNanos) < 50.0,
                () -> "Expected substring-filtered MySQL pagination average below 50 ms for 10,000 rows; observed "
                        + String.format(Locale.ROOT, "%.3f ms", averageMillis(filteredNanos)));
    }

    private void printQueryPlans(UUID curriculumId) {
        String unfilteredCountSql = """
                SELECT COUNT(e.entry_id) AS total_items
                FROM academic_curriculum c
                LEFT JOIN academic_curriculum_entry e ON e.curriculum_id = c.curriculum_id
                WHERE c.curriculum_id = ? AND c.status = 'PUBLISHED'
                GROUP BY c.curriculum_id
                """;
        String filteredCountSql = """
                SELECT COUNT(e.entry_id) AS total_items
                FROM academic_curriculum c
                LEFT JOIN academic_curriculum_entry e ON e.curriculum_id = c.curriculum_id AND %s
                WHERE c.curriculum_id = ? AND c.status = 'PUBLISHED'
                GROUP BY c.curriculum_id
                """;
        String pageSql = """
                SELECT e.subject_id, e.subject_revision_id, s.subject_code, sr.subject_name, sr.credits,
                       e.semester, e.formation_space, e.component, e.choice_group, e.row_order
                FROM academic_curriculum c
                JOIN academic_curriculum_entry e ON e.curriculum_id = c.curriculum_id
                JOIN academic_subject s ON s.subject_id = e.subject_id
                JOIN academic_subject_revision sr
                  ON sr.subject_revision_id = e.subject_revision_id AND sr.subject_id = e.subject_id
                WHERE c.curriculum_id = ? AND c.status = 'PUBLISHED' AND %s
                ORDER BY e.semester, e.row_order
                LIMIT ? OFFSET ?
                """;
        String filter = "(LOWER(e.search_subject_code) LIKE LOWER(?) ESCAPE '!' "
                + "OR LOWER(e.search_subject_name) LIKE LOWER(?) ESCAPE '!')";
        String searchPattern = "%09999%";
        printPlan("unfiltered-count", unfilteredCountSql, curriculumId.toString());
        printPlan("unfiltered-page", pageSql.formatted("1 = 1"), curriculumId.toString(), 100, 0L);
        printPlan("filtered-count", filteredCountSql.formatted(filter), searchPattern, searchPattern,
                curriculumId.toString());
        printPlan("filtered-page", pageSql.formatted(filter), curriculumId.toString(), searchPattern,
                searchPattern, 100, 0L);
    }

    private void printPlan(String name, String sql, Object... parameters) {
        String plan = jdbcTemplate.queryForObject("EXPLAIN ANALYZE " + sql, String.class, parameters);
        System.out.println("MYSQL_EXPLAIN scenario=" + name + System.lineSeparator() + plan);
    }

    private List<Long> sampleQuery(UUID curriculumId, CurriculumEntriesPageQuery query) {
        for (int warmup = 0; warmup < PERFORMANCE_WARMUPS; warmup++) {
            queryService.publishedCurriculumEntries(curriculumId, query);
        }

        List<Long> samples = new ArrayList<>(PERFORMANCE_SAMPLES);
        for (int sample = 0; sample < PERFORMANCE_SAMPLES; sample++) {
            long started = System.nanoTime();
            AcademicCurriculumEntriesPage result = queryService.publishedCurriculumEntries(curriculumId, query);
            samples.add(System.nanoTime() - started);
            assertTrue(result.entries().size() <= CurriculumEntriesPageQuery.MAX_PAGE_SIZE);
        }
        return samples;
    }

    private UUID seedPerformanceCurriculum(int entryCount) {
        UUID programId = UUID.randomUUID();
        UUID programRevisionId = UUID.randomUUID();
        UUID curriculumId = UUID.randomUUID();
        String programCode = "PERF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                .toUpperCase(Locale.ROOT);
        String fingerprint = "a".repeat(64);
        String sourceHash = "b".repeat(64);
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC).withNano(0);

        jdbcTemplate.update("""
                INSERT INTO academic_program
                    (program_id, program_code, academic_level, study_modality, campus_code, created_at)
                VALUES (?, ?, 'PREGRADO', 'PRESENCIAL', 'PERF', ?)
                """, programId.toString(), programCode, createdAt);
        jdbcTemplate.update("""
                INSERT INTO academic_program_revision
                    (program_revision_id, program_id, content_fingerprint, snies_code, program_name, faculty,
                     campus_name, created_at)
                VALUES (?, ?, ?, NULL, 'Synthetic benchmark program', 'Synthetic faculty', 'Synthetic campus', ?)
                """, programRevisionId.toString(), programId.toString(), fingerprint, createdAt);
        jdbcTemplate.update("""
                INSERT INTO academic_curriculum
                    (curriculum_id, program_id, program_revision_id, curriculum_version, cohort_from,
                     cohort_through, approval_reference, status, source_sha256, created_by, created_at,
                     published_by, published_at)
                VALUES (?, ?, ?, 'BENCHMARK-1', '2026-1', NULL, 'Synthetic benchmark fixture', 'PUBLISHED',
                        ?, 'synthetic-mysql-performance-probe', ?, 'synthetic-mysql-performance-probe', ?)
                """, curriculumId.toString(), programId.toString(), programRevisionId.toString(), sourceHash,
                createdAt, createdAt);

        List<SubjectFixture> subjects = new ArrayList<>(entryCount);
        for (int row = 1; row <= entryCount; row++) {
            subjects.add(new SubjectFixture(UUID.randomUUID(), UUID.randomUUID(), row));
        }

        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_subject (subject_id, subject_code, created_at)
                VALUES (?, ?, ?)
                """, subjects, 500, (statement, subject) -> {
            statement.setString(1, subject.subjectId().toString());
            statement.setString(2, String.format(Locale.ROOT, "MAT%05d", subject.rowOrder()));
            statement.setTimestamp(3, Timestamp.valueOf(createdAt));
        });
        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_subject_revision
                    (subject_revision_id, subject_id, content_fingerprint, subject_name, credits, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, subjects, 500, (statement, subject) -> {
            statement.setString(1, subject.revisionId().toString());
            statement.setString(2, subject.subjectId().toString());
            statement.setString(3, String.format(Locale.ROOT, "%064x", subject.rowOrder()));
            statement.setString(4, String.format(Locale.ROOT, "Materia sintética %05d", subject.rowOrder()));
            statement.setBigDecimal(5, new BigDecimal("3.00"));
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
        });
        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_curriculum_entry
                    (curriculum_id, subject_id, subject_revision_id, semester, formation_space, component,
                     choice_group, row_order, search_subject_code, search_subject_name)
                VALUES (?, ?, ?, ?, 'Synthetic foundation', 'Required', NULL, ?, ?, ?)
                """, subjects, 500, (statement, subject) -> {
            statement.setString(1, curriculumId.toString());
            statement.setString(2, subject.subjectId().toString());
            statement.setString(3, subject.revisionId().toString());
            statement.setInt(4, (subject.rowOrder() - 1) % 10 + 1);
            statement.setInt(5, subject.rowOrder());
            statement.setString(6, String.format(Locale.ROOT, "MAT%05d", subject.rowOrder()));
            statement.setString(7, String.format(Locale.ROOT, "Materia sintética %05d", subject.rowOrder()));
        });
        return curriculumId;
    }

    private int countCurricula(UUID curriculumId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_curriculum WHERE curriculum_id = ?",
                Integer.class,
                curriculumId.toString());
    }

    private static void printMetrics(String scenario, List<Long> nanos) {
        long[] sorted = nanos.stream().mapToLong(Long::longValue).sorted().toArray();
        System.out.printf(Locale.ROOT,
                "MYSQL_PERF scenario=%s rows=%d page_size=%d concurrency=1 warmups=%d samples=%d "
                        + "avg_ms=%.3f p50_ms=%.3f p95_ms=%.3f p99_ms=%.3f%n",
                scenario, PERFORMANCE_ROWS, CurriculumEntriesPageQuery.MAX_PAGE_SIZE,
                PERFORMANCE_WARMUPS, sorted.length, averageMillis(nanos),
                percentileMillis(sorted, 0.50), percentileMillis(sorted, 0.95), percentileMillis(sorted, 0.99));
    }

    private static double averageMillis(List<Long> nanos) {
        return nanos.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
    }

    private static double percentileMillis(long[] sortedNanos, double percentile) {
        int index = Math.max(0, (int) Math.ceil(percentile * sortedNanos.length) - 1);
        return sortedNanos[index] / 1_000_000.0;
    }

    private static String row(
            String programCode,
            String version,
            String cohortFrom,
            String subjectCode,
            String subjectName,
            String semester
    ) {
        return String.join(",", programCode, "PREGRADO", "PRESENCIAL", "", "Synthetic program",
                "Synthetic faculty", "TUNJA", "Tunja", version, cohortFrom, "", "Synthetic approval reference",
                semester, subjectCode, subjectName, "3.00", "Synthetic foundation", "Required", "");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " when enabling the opt-in MySQL contract tests.");
        }
        return value;
    }

    private record SubjectFixture(UUID subjectId, UUID revisionId, int rowOrder) {
    }
}
