package co.edu.uptc.universiry.academics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.h2.jdbcx.JdbcDataSource;

import java.time.LocalDate;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-structure-schema;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class AcademicStructureSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flyway_creates_normalized_academic_structure_tables_and_serialization_row() {
        // Arrange
        String[] tables = {
                "academic_organization_unit",
                "academic_organization_relation",
                "academic_site",
                "academic_site_relation",
                "academic_program_affiliation",
                "academic_structure_audit_event"
        };

        // Act + Assert
        for (String table : tables) {
            assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_control WHERE control_id = 1", Integer.class));
    }

    @Test
    void organization_relation_order_defaults_to_zero_and_rejects_negative_values() {
        // Arrange
        String parentId = UUID.randomUUID().toString();
        String childId = UUID.randomUUID().toString();
        insertUnit(parentId, "FAC-" + parentId.substring(0, 8), "FACULTY");
        insertUnit(childId, "SCH-" + childId.substring(0, 8), "SCHOOL");

        // Act
        jdbcTemplate.update("""
                INSERT INTO academic_organization_relation (parent_unit_id, child_unit_id, valid_from, valid_through)
                VALUES (?, ?, ?, NULL)
                """, parentId, childId, LocalDate.of(2026, 1, 1));

        // Assert
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT display_order FROM academic_organization_relation
                WHERE parent_unit_id = ? AND child_unit_id = ?
                """, Integer.class, parentId, childId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO academic_organization_relation
                    (parent_unit_id, child_unit_id, display_order, valid_from, valid_through)
                VALUES (?, ?, -1, ?, NULL)
                """, parentId, childId, LocalDate.of(2027, 1, 1)));
    }

    @Test
    void site_relation_order_defaults_to_zero_and_rejects_negative_values() {
        // Arrange
        String parentId = UUID.randomUUID().toString();
        String childId = UUID.randomUUID().toString();
        insertSite(parentId, "SITE-" + parentId.substring(0, 8));
        insertSite(childId, "SITE-" + childId.substring(0, 8));

        // Act
        jdbcTemplate.update("""
                INSERT INTO academic_site_relation (parent_site_id, child_site_id, valid_from, valid_through)
                VALUES (?, ?, ?, NULL)
                """, parentId, childId, LocalDate.of(2026, 1, 1));

        // Assert
        assertEquals(0, jdbcTemplate.queryForObject("""
                SELECT display_order FROM academic_site_relation
                WHERE parent_site_id = ? AND child_site_id = ?
                """, Integer.class, parentId, childId));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO academic_site_relation
                    (parent_site_id, child_site_id, display_order, valid_from, valid_through)
                VALUES (?, ?, -1, ?, NULL)
                """, parentId, childId, LocalDate.of(2027, 1, 1)));
    }

    @Test
    void structure_audit_schema_accepts_order_changes_and_rejects_unknown_actions() {
        // Arrange
        String entityId = UUID.randomUUID().toString();
        String[] actions = {
                "UNIT_ORDER_CHANGED",
                "SITE_ORDER_CHANGED",
                "UNIT_RELATION_ORDER_CHANGED",
                "SITE_RELATION_ORDER_CHANGED",
                "PROGRAM_ORDER_CHANGED"
        };

        // Act
        for (String action : actions) {
            jdbcTemplate.update("""
                    INSERT INTO academic_structure_audit_event
                        (entity_id, action_key, actor_sub, source_reference, occurred_at, event_summary)
                    VALUES (?, ?, 'structure.operator', 'Ajuste de orden', CURRENT_TIMESTAMP, 'Orden 8 -> 2')
                    """, entityId, action);
        }

        // Assert
        assertEquals(actions.length, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM academic_structure_audit_event WHERE source_reference = ?",
                Integer.class, "Ajuste de orden"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO academic_structure_audit_event
                    (entity_id, action_key, actor_sub, source_reference, occurred_at, event_summary)
                VALUES (?, 'UNKNOWN_ORDER_CHANGED', 'structure.operator', 'Referencia', CURRENT_TIMESTAMP, 'Orden')
                """, UUID.randomUUID().toString()));
    }

    @Test
    void v10_backfills_existing_relation_order_from_child_node_order() throws Exception {
        // Arrange
        Path databaseDirectory = Files.createTempDirectory("academic-structure-v10-backfill-");
        JdbcDataSource migrationDataSource = new JdbcDataSource();
        migrationDataSource.setURL("jdbc:h2:file:" + databaseDirectory.resolve("academic-structure")
                .toAbsolutePath().toString().replace('\\', '/') + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        migrationDataSource.setUser("sa");
        migrationDataSource.setPassword("");
        try {
            Flyway.configure().dataSource(migrationDataSource).locations("classpath:db/migration")
                    .target(MigrationVersion.fromVersion("9")).load().migrate();
            JdbcTemplate migrationJdbc = new JdbcTemplate(migrationDataSource);

        String parentUnitId = UUID.randomUUID().toString();
        String childUnitId = UUID.randomUUID().toString();
        String parentSiteId = UUID.randomUUID().toString();
        String childSiteId = UUID.randomUUID().toString();
        insertUnit(migrationJdbc, parentUnitId, "FAC-MIGRATION-" + parentUnitId.substring(0, 8), 0);
        insertUnit(migrationJdbc, childUnitId, "SCH-MIGRATION-" + childUnitId.substring(0, 8), 6, "SCHOOL");
        insertSite(migrationJdbc, parentSiteId, "SITE-P-MIGRATION-" + parentSiteId.substring(0, 8), 0, "CENTRAL");
        insertSite(migrationJdbc, childSiteId, "SITE-C-MIGRATION-" + childSiteId.substring(0, 8), 9, "REGIONAL");
        migrationJdbc.update("""
                INSERT INTO academic_organization_relation (parent_unit_id, child_unit_id, valid_from, valid_through)
                VALUES (?, ?, ?, NULL)
                """, parentUnitId, childUnitId, LocalDate.of(2026, 1, 1));
        migrationJdbc.update("""
                INSERT INTO academic_site_relation (parent_site_id, child_site_id, valid_from, valid_through)
                VALUES (?, ?, ?, NULL)
                """, parentSiteId, childSiteId, LocalDate.of(2026, 1, 1));

        // Act
            Flyway.configure().dataSource(migrationDataSource).locations("classpath:db/migration")
                    .load().migrate();

        // Assert
            assertEquals(6, migrationJdbc.queryForObject("""
                SELECT display_order FROM academic_organization_relation
                WHERE parent_unit_id = ? AND child_unit_id = ?
                """, Integer.class, parentUnitId, childUnitId));
            assertEquals(9, migrationJdbc.queryForObject("""
                SELECT display_order FROM academic_site_relation
                WHERE parent_site_id = ? AND child_site_id = ?
                """, Integer.class, parentSiteId, childSiteId));
        } finally {
            try (var paths = Files.walk(databaseDirectory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
            }
        }
    }

    private void insertUnit(String id, String code, String type) {
        insertUnit(jdbcTemplate, id, code, 0, type);
    }

    private static void insertUnit(JdbcTemplate jdbc, String id, String code, int order) {
        insertUnit(jdbc, id, code, order, "FACULTY");
    }

    private static void insertUnit(JdbcTemplate jdbc, String id, String code, int order, String type) {
        jdbc.update("""
                INSERT INTO academic_organization_unit
                    (organization_unit_id, unit_code, unit_type, display_name, display_order, status,
                     valid_from, valid_through, created_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, NULL, CURRENT_TIMESTAMP)
                """, id, code, type, code, order, LocalDate.of(2026, 1, 1));
    }

    private void insertSite(String id, String code) {
        insertSite(jdbcTemplate, id, code, 0);
    }

    private static void insertSite(JdbcTemplate jdbc, String id, String code, int order) {
        insertSite(jdbc, id, code, order, "CENTRAL");
    }

    private static void insertSite(JdbcTemplate jdbc, String id, String code, int order, String type) {
        jdbc.update("""
                INSERT INTO academic_site
                    (site_id, site_code, site_type, display_name, display_order, status,
                     valid_from, valid_through, created_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, NULL, CURRENT_TIMESTAMP)
                """, id, code, type, code, order, LocalDate.of(2026, 1, 1));
    }
}
