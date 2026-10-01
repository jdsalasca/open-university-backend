package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicStructureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Optional local/MySQL contract for the public structure read used by the catalog.
 * Fixtures are synthetic and the enclosing transaction rolls back after the test.
 */
@EnabledIfSystemProperty(named = "universiry.mysql-contract.enabled", matches = "true")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AcademicStructureMySqlPerformanceContractTest {

    private static final int FACULTY_COUNT = 10;
    private static final int UNIT_COUNT = 100;
    private static final int SITE_COUNT = 20;
    private static final int PROGRAM_COUNT = 1_000;
    private static final int WARMUPS = 10;
    private static final int SAMPLES = 50;
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2000, 1, 1);

    @Autowired
    private AcademicStructureService structureService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    private String programCodePrefix;
    private String unitCodePrefix;
    private String siteCodePrefix;

    @DynamicPropertySource
    static void mysqlDatasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_URL"));
        properties.add("spring.datasource.username", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_USERNAME"));
        properties.add("spring.datasource.password", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Test
    void measures_public_structure_repository_and_json_api_with_synthetic_rows() throws Exception {
        // Arrange
        String fixtureTag = "STRUCTURE-PERF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10)
                .toUpperCase(Locale.ROOT);
        programCodePrefix = fixtureTag + "-P-";
        unitCodePrefix = fixtureTag + "-U-";
        siteCodePrefix = fixtureTag + "-S-";
        List<UUID> unitIds = insertUnits();
        List<UUID> siteIds = insertSites();
        insertRelations(unitIds, siteIds);
        insertProgramsAndAffiliations(unitIds, siteIds);

        var snapshot = structureService.publicStructure();
        assertEquals(UNIT_COUNT, snapshot.units().size());
        assertEquals(SITE_COUNT, snapshot.sites().size());
        assertEquals(PROGRAM_COUNT, snapshot.programAffiliations().size());
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.units.length()").value(UNIT_COUNT))
                .andExpect(jsonPath("$.sites.length()").value(SITE_COUNT))
                .andExpect(jsonPath("$.programAffiliations.length()").value(PROGRAM_COUNT));

        // Act
        Measurement repository = measureRepositoryRead();
        Measurement jsonApi = measureJsonApi();

        // Assert
        report("repository-jdbc", repository);
        report("mockmvc-json-api", jsonApi);
        assertTrue(repository.meanMillis() < 50.0,
                () -> "Expected the public structure repository read average below 50 ms for " + PROGRAM_COUNT
                        + " synthetic affiliations; observed " + repository.meanMillis() + " ms");
        assertTrue(jsonApi.meanMillis() < 50.0,
                () -> "Expected the public structure JSON API average below 50 ms for " + PROGRAM_COUNT
                        + " synthetic affiliations; observed " + jsonApi.meanMillis() + " ms");
    }

    @AfterTransaction
    void rolls_back_all_synthetic_structure_rows() {
        // Assert
        assertEquals(0, count("SELECT COUNT(*) FROM academic_program WHERE program_code LIKE ?",
                programCodePrefix + "%"));
        assertEquals(0, count("SELECT COUNT(*) FROM academic_organization_unit WHERE unit_code LIKE ?",
                unitCodePrefix + "%"));
        assertEquals(0, count("SELECT COUNT(*) FROM academic_site WHERE site_code LIKE ?", siteCodePrefix + "%"));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM academic_program_affiliation a
                JOIN academic_program p ON p.program_id = a.program_id
                WHERE p.program_code LIKE ?
                """, programCodePrefix + "%"));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM academic_organization_relation r
                JOIN academic_organization_unit child ON child.organization_unit_id = r.child_unit_id
                WHERE child.unit_code LIKE ?
                """, unitCodePrefix + "%"));
        assertEquals(0, count("""
                SELECT COUNT(*) FROM academic_site_relation r
                JOIN academic_site child ON child.site_id = r.child_site_id
                WHERE child.site_code LIKE ?
                """, siteCodePrefix + "%"));
    }

    private List<UUID> insertUnits() {
        List<UUID> unitIds = new ArrayList<>(UNIT_COUNT);
        for (int index = 0; index < UNIT_COUNT; index++) unitIds.add(UUID.randomUUID());
        List<IndexedEntity> units = new ArrayList<>(UNIT_COUNT);
        for (int index = 0; index < UNIT_COUNT; index++) units.add(new IndexedEntity(unitIds.get(index), index));
        Timestamp createdAt = Timestamp.from(Instant.now());
        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_organization_unit
                    (organization_unit_id, unit_code, unit_type, display_name, display_order, status,
                     valid_from, valid_through, created_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, NULL, ?)
                """, units, 100, (statement, unit) -> {
            int index = unit.index();
            statement.setString(1, unit.id().toString());
            statement.setString(2, unitCodePrefix + String.format(Locale.ROOT, "%04d", index));
            statement.setString(3, index < FACULTY_COUNT ? "FACULTY" : "ACADEMIC_UNIT");
            statement.setString(4, "Synthetic academic unit " + index);
            statement.setInt(5, index);
            statement.setObject(6, EFFECTIVE_FROM);
            statement.setTimestamp(7, createdAt);
        });
        return unitIds;
    }

    private List<UUID> insertSites() {
        List<UUID> siteIds = new ArrayList<>(SITE_COUNT);
        for (int index = 0; index < SITE_COUNT; index++) siteIds.add(UUID.randomUUID());
        List<IndexedEntity> sites = new ArrayList<>(SITE_COUNT);
        for (int index = 0; index < SITE_COUNT; index++) sites.add(new IndexedEntity(siteIds.get(index), index));
        Timestamp createdAt = Timestamp.from(Instant.now());
        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_site
                    (site_id, site_code, site_type, display_name, display_order, status,
                     valid_from, valid_through, created_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, NULL, ?)
                """, sites, 100, (statement, site) -> {
            int index = site.index();
            statement.setString(1, site.id().toString());
            statement.setString(2, siteCodePrefix + String.format(Locale.ROOT, "%03d", index));
            statement.setString(3, index == 0 ? "CENTRAL" : "SECCIONAL");
            statement.setString(4, "Synthetic academic site " + index);
            statement.setInt(5, index);
            statement.setObject(6, EFFECTIVE_FROM);
            statement.setTimestamp(7, createdAt);
        });
        return siteIds;
    }

    private void insertRelations(List<UUID> unitIds, List<UUID> siteIds) {
        Timestamp createdAt = Timestamp.from(Instant.now());
        List<RelationRow> unitRelations = new ArrayList<>(UNIT_COUNT - FACULTY_COUNT);
        for (int index = FACULTY_COUNT; index < UNIT_COUNT; index++) {
            unitRelations.add(new RelationRow(unitIds.get(index % FACULTY_COUNT), unitIds.get(index), index));
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_organization_relation
                    (parent_unit_id, child_unit_id, display_order, valid_from, valid_through)
                VALUES (?, ?, ?, ?, NULL)
                """, unitRelations, 100, (statement, relation) -> {
            statement.setString(1, relation.parentId().toString());
            statement.setString(2, relation.childId().toString());
            statement.setInt(3, relation.displayOrder());
            statement.setObject(4, EFFECTIVE_FROM);
        });

        List<RelationRow> siteRelations = new ArrayList<>(SITE_COUNT - 1);
        for (int index = 1; index < SITE_COUNT; index++) {
            siteRelations.add(new RelationRow(siteIds.get(0), siteIds.get(index), index));
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_site_relation
                    (parent_site_id, child_site_id, display_order, valid_from, valid_through)
                VALUES (?, ?, ?, ?, NULL)
                """, siteRelations, 100, (statement, relation) -> {
            statement.setString(1, relation.parentId().toString());
            statement.setString(2, relation.childId().toString());
            statement.setInt(3, relation.displayOrder());
            statement.setObject(4, EFFECTIVE_FROM);
        });
    }

    private void insertProgramsAndAffiliations(List<UUID> unitIds, List<UUID> siteIds) {
        List<ProgramRow> programs = new ArrayList<>(PROGRAM_COUNT);
        for (int index = 0; index < PROGRAM_COUNT; index++) programs.add(new ProgramRow(UUID.randomUUID(), index));
        Timestamp createdAt = Timestamp.from(Instant.now());
        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_program
                    (program_id, program_code, academic_level, study_modality, campus_code, created_at)
                VALUES (?, ?, 'PREGRADO', 'PRESENCIAL', ?, ?)
                """, programs, 250, (statement, program) -> {
            statement.setString(1, program.id().toString());
            statement.setString(2, programCodePrefix + String.format(Locale.ROOT, "%04d", program.index()));
            statement.setString(3, siteCodePrefix + String.format(Locale.ROOT, "%03d", program.index() % SITE_COUNT));
            statement.setTimestamp(4, createdAt);
        });

        jdbcTemplate.batchUpdate("""
                INSERT INTO academic_program_affiliation
                    (affiliation_id, program_id, organization_unit_id, site_id, display_order,
                     valid_from, valid_through, source_reference, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)
                """, programs, 250, (statement, program) -> {
            int unitIndex = FACULTY_COUNT + program.index() % (UNIT_COUNT - FACULTY_COUNT);
            int siteIndex = program.index() % SITE_COUNT;
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, program.id().toString());
            statement.setString(3, unitIds.get(unitIndex).toString());
            statement.setString(4, siteIds.get(siteIndex).toString());
            statement.setInt(5, program.index());
            statement.setObject(6, EFFECTIVE_FROM);
            statement.setString(7, "Synthetic performance profile");
            statement.setString(8, "synthetic-performance-test");
            statement.setTimestamp(9, createdAt);
        });
    }

    private Measurement measureRepositoryRead() {
        for (int warmup = 0; warmup < WARMUPS; warmup++) structureService.publicStructure();
        List<Long> durations = new ArrayList<>(SAMPLES);
        for (int sample = 0; sample < SAMPLES; sample++) {
            long startedAt = System.nanoTime();
            var snapshot = structureService.publicStructure();
            durations.add(System.nanoTime() - startedAt);
            assertEquals(PROGRAM_COUNT, snapshot.programAffiliations().size());
        }
        return Measurement.from(durations);
    }

    private Measurement measureJsonApi() throws Exception {
        for (int warmup = 0; warmup < WARMUPS; warmup++) requestPublicStructure();
        List<Long> durations = new ArrayList<>(SAMPLES);
        for (int sample = 0; sample < SAMPLES; sample++) {
            long startedAt = System.nanoTime();
            MvcResult result = mockMvc.perform(get("/api/v1/academic-structure")).andReturn();
            durations.add(System.nanoTime() - startedAt);
            assertEquals(200, result.getResponse().getStatus());
            assertFalse(result.getResponse().getContentAsString().isBlank());
        }
        return Measurement.from(durations);
    }

    private void requestPublicStructure() throws Exception {
        mockMvc.perform(get("/api/v1/academic-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.programAffiliations.length()").value(PROGRAM_COUNT));
    }

    private static void report(String scenario, Measurement measurement) {
        System.out.printf(Locale.ROOT,
                "ACADEMIC_STRUCTURE_PERFORMANCE scenario=%s units=%d sites=%d affiliations=%d warmups=%d "
                        + "samples=%d concurrency=1 mean_ms=%.3f p50_ms=%.3f p95_ms=%.3f p99_ms=%.3f%n",
                scenario, UNIT_COUNT, SITE_COUNT, PROGRAM_COUNT, WARMUPS, SAMPLES,
                measurement.meanMillis(), measurement.p50Millis(), measurement.p95Millis(), measurement.p99Millis());
    }

    private int count(String sql, Object parameter) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, parameter);
        return count == null ? -1 : count;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing required environment variable " + name);
        return value;
    }

    private record ProgramRow(UUID id, int index) { }

    private record IndexedEntity(UUID id, int index) { }

    private record RelationRow(UUID parentId, UUID childId, int displayOrder) { }

    private record Measurement(double meanMillis, double p50Millis, double p95Millis, double p99Millis) {
        static Measurement from(List<Long> durations) {
            List<Long> ordered = new ArrayList<>(durations);
            Collections.sort(ordered);
            double mean = durations.stream().mapToLong(Long::longValue).average().orElseThrow() / 1_000_000.0;
            return new Measurement(mean, percentileMillis(ordered, 0.50), percentileMillis(ordered, 0.95),
                    percentileMillis(ordered, 0.99));
        }

        private static double percentileMillis(List<Long> ordered, double percentile) {
            int index = (int) Math.ceil(percentile * ordered.size()) - 1;
            return ordered.get(index) / 1_000_000.0;
        }
    }
}
