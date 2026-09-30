package co.edu.uptc.universiry.academics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
