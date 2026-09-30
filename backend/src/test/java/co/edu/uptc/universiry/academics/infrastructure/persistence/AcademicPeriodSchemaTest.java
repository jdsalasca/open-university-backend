package co.edu.uptc.universiry.academics.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-period-schema;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class AcademicPeriodSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flyway_creates_period_calendar_and_audit_tables_without_seed_periods() {
        // Arrange
        String[] tables = {"academic_period", "academic_calendar_revision", "academic_calendar_activity",
                "academic_period_audit_event"};

        // Act + Assert
        for (String table : tables) {
            assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
    }
}
