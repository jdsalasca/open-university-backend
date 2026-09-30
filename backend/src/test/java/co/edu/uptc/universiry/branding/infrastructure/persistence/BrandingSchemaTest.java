package co.edu.uptc.universiry.branding.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.flyway.enabled=true")
@ActiveProfiles("test")
class BrandingSchemaTest {

    private static final Set<String> EXPECTED_TABLES = Set.of(
            "institution_branding_current",
            "institution_branding_revision",
            "institution_color_token",
            "institution_module_label",
            "institution_banner",
            "media_asset",
            "administrative_audit_event"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void branding_schema_is_available_after_flyway_migration() {
        // Arrange
        Set<String> expectedTables = EXPECTED_TABLES;

        // Act
        Set<String> actualTables = jdbcTemplate.execute((ConnectionCallback<Set<String>>) connection -> readTableNames(connection.getMetaData().getTables(
                connection.getCatalog(), null, "%", new String[]{"TABLE"}
        )));

        // Assert
        assertTrue(actualTables.containsAll(expectedTables), () -> "Missing tables: " + difference(expectedTables, actualTables));
        assertEquals("#FFCC29", jdbcTemplate.queryForObject(
                "select color_hex from institution_color_token where revision_id = 1 and token_key = 'primary'", String.class
        ));
        assertEquals("#1A1A1A", jdbcTemplate.queryForObject(
                "select color_hex from institution_color_token where revision_id = 1 and token_key = 'ink'", String.class
        ));
    }

    private static Set<String> readTableNames(ResultSet tables) throws SQLException {
        Set<String> names = new TreeSet<>();
        try (tables) {
            while (tables.next()) {
                names.add(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }
}
