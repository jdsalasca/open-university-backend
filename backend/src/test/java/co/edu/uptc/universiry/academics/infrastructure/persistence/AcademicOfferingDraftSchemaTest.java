package co.edu.uptc.universiry.academics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-offering-schema;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class AcademicOfferingDraftSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flyway_creates_empty_draft_and_append_only_audit_tables() {
        // Arrange
        String[] tables = {"academic_offering_draft", "academic_offering_draft_audit_event"};

        // Act + Assert
        for (String table : tables) {
            assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
    }
}
