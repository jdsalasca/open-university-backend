package co.edu.uptc.universiry.academics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.flyway.enabled=true")
@ActiveProfiles("test")
class AcademicCatalogSchemaTest {

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "academic_program",
            "academic_program_revision",
            "academic_subject",
            "academic_subject_revision",
            "academic_curriculum",
            "academic_curriculum_entry",
            "academic_catalog_audit_event"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flyway_adds_the_seven_academic_catalog_tables_without_replacing_branding_tables() {
        // Arrange
        Set<String> expected = new TreeSet<>(EXPECTED_TABLES);
        expected.addAll(Set.of(
                "institution_branding_current",
                "institution_branding_revision",
                "institution_color_token",
                "institution_module_label",
                "institution_banner",
                "media_asset",
                "administrative_audit_event"));

        // Act
        Set<String> actual = jdbcTemplate.execute((ConnectionCallback<Set<String>>) connection ->
                tableNames(connection.getMetaData(), connection.getCatalog()));
        Integer successfulAcademicMigration = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = TRUE", Integer.class);

        // Assert
        assertTrue(actual.containsAll(expected), () -> "Missing tables: " + difference(expected, actual));
        assertEquals(1, successfulAcademicMigration);
    }

    @Test
    void catalog_foreign_keys_keep_program_subject_curriculum_and_audit_records_attached() {
        // Arrange
        Set<String> expectedRelationships = Set.of(
                "academic_program_revision>academic_program",
                "academic_subject_revision>academic_subject",
                "academic_curriculum>academic_program",
                "academic_curriculum>academic_program_revision",
                "academic_curriculum_entry>academic_curriculum",
                "academic_curriculum_entry>academic_subject_revision",
                "academic_catalog_audit_event>academic_curriculum");

        // Act
        Set<String> actualRelationships = jdbcTemplate.execute((ConnectionCallback<Set<String>>) connection ->
                foreignKeyRelationships(connection.getMetaData(), connection.getCatalog(), EXPECTED_TABLES));

        // Assert
        assertTrue(actualRelationships.containsAll(expectedRelationships),
                () -> "Missing foreign keys: " + difference(expectedRelationships, actualRelationships));
    }

    @Test
    void catalog_unique_indexes_enforce_stable_identity_and_revision_reuse() {
        // Arrange
        Map<String, Set<Set<String>>> uniqueColumns = jdbcTemplate.execute(
                (ConnectionCallback<Map<String, Set<Set<String>>>>) connection -> {
                    Map<String, Set<Set<String>>> result = new HashMap<>();
                    for (String table : EXPECTED_TABLES) {
                        result.put(table, indexColumnSets(connection.getMetaData(), connection.getCatalog(), table, true));
                    }
                    return result;
                });

        // Act
        Set<Set<String>> programIndexes = uniqueColumns.get("academic_program");
        Set<Set<String>> programRevisionIndexes = uniqueColumns.get("academic_program_revision");
        Set<Set<String>> subjectIndexes = uniqueColumns.get("academic_subject");
        Set<Set<String>> subjectRevisionIndexes = uniqueColumns.get("academic_subject_revision");
        Set<Set<String>> curriculumIndexes = uniqueColumns.get("academic_curriculum");
        Set<Set<String>> entryIndexes = uniqueColumns.get("academic_curriculum_entry");

        // Assert
        assertTrue(programIndexes.contains(Set.of("program_code", "academic_level", "study_modality", "campus_code")));
        assertTrue(programRevisionIndexes.contains(Set.of("program_id", "content_fingerprint")));
        assertTrue(subjectIndexes.contains(Set.of("subject_code")));
        assertTrue(subjectRevisionIndexes.contains(Set.of("subject_id", "content_fingerprint")));
        assertTrue(curriculumIndexes.contains(Set.of("program_id", "curriculum_version")));
        assertTrue(entryIndexes.contains(Set.of("curriculum_id", "subject_id")));
    }

    @Test
    void catalog_query_indexes_cover_publication_entry_order_and_audit_lookups() {
        // Arrange
        Map<String, Set<Set<String>>> indexColumns = jdbcTemplate.execute(
                (ConnectionCallback<Map<String, Set<Set<String>>>>) connection -> {
                    Map<String, Set<Set<String>>> result = new HashMap<>();
                    for (String table : EXPECTED_TABLES) {
                        result.put(table, indexColumnSets(connection.getMetaData(), connection.getCatalog(), table, false));
                    }
                    return result;
                });

        // Act
        Set<Set<String>> curriculumIndexes = indexColumns.get("academic_curriculum");
        Set<Set<String>> entryIndexes = indexColumns.get("academic_curriculum_entry");
        Set<Set<String>> auditIndexes = indexColumns.get("academic_catalog_audit_event");

        // Assert
        assertTrue(curriculumIndexes.contains(Set.of("status", "program_id", "cohort_from", "cohort_through")));
        assertTrue(curriculumIndexes.contains(Set.of("status", "created_at")));
        assertTrue(entryIndexes.contains(Set.of("curriculum_id", "semester", "row_order")));
        assertTrue(auditIndexes.contains(Set.of("curriculum_id", "occurred_at")));
    }

    @Test
    void catalog_check_constraints_reject_unsupported_scope_and_invalid_lifecycle_values() {
        // Arrange
        Set<String> expectedChecks = Set.of(
                "ck_academic_program_scope",
                "ck_academic_subject_revision_credits",
                "ck_academic_curriculum_status",
                "ck_academic_curriculum_cohort_through",
                "ck_academic_curriculum_publication_state",
                "ck_academic_curriculum_entry_semester",
                "ck_academic_catalog_audit_action");

        // Act
        Set<String> actualChecks = Set.copyOf(jdbcTemplate.query(
                "SELECT LOWER(constraint_name) FROM information_schema.table_constraints "
                        + "WHERE constraint_type = 'CHECK'",
                (resultSet, rowNumber) -> resultSet.getString(1)));

        // Assert
        assertTrue(actualChecks.containsAll(expectedChecks),
                () -> "Missing check constraints: " + difference(expectedChecks, actualChecks));
    }

    private static Set<String> tableNames(DatabaseMetaData metadata, String catalog) throws SQLException {
        Set<String> names = new TreeSet<>();
        try (ResultSet tables = metadata.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                names.add(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static Set<String> foreignKeyRelationships(
            DatabaseMetaData metadata,
            String catalog,
            Set<String> tables
    ) throws SQLException {
        Set<String> relationships = new HashSet<>();
        for (String table : tables) {
            try (ResultSet keys = metadata.getImportedKeys(catalog, null, table)) {
                while (keys.next()) {
                    relationships.add(keys.getString("FKTABLE_NAME").toLowerCase(Locale.ROOT)
                            + ">" + keys.getString("PKTABLE_NAME").toLowerCase(Locale.ROOT));
                }
            }
        }
        return relationships;
    }

    private static Set<Set<String>> indexColumnSets(
            DatabaseMetaData metadata,
            String catalog,
            String table,
            boolean uniqueOnly
    ) throws SQLException {
        Map<String, List<String>> indexes = new HashMap<>();
        try (ResultSet rows = metadata.getIndexInfo(catalog, null, table, uniqueOnly, false)) {
            while (rows.next()) {
                String indexName = rows.getString("INDEX_NAME");
                String column = rows.getString("COLUMN_NAME");
                if (indexName != null && column != null) {
                    indexes.computeIfAbsent(indexName, ignored -> new ArrayList<>())
                            .add(column.toLowerCase(Locale.ROOT));
                }
            }
        }
        Set<Set<String>> columnSets = new HashSet<>();
        indexes.values().forEach(columns -> columnSets.add(Set.copyOf(columns)));
        return columnSets;
    }

    private static Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }
}
