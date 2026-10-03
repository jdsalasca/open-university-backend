package co.edu.uptc.universiry.notices.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:institutional-notice-schema;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
@Transactional
class InstitutionalNoticeSchemaTest {

    private static final String NOTICE_ID = "b1f6a0f4-6c1e-4a51-9a1b-2d5f1f0a9c31";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flyway_creates_empty_notice_audience_and_audit_tables() {
        // Arrange
        String[] tables = {"institutional_notice", "institutional_notice_audience", "institutional_notice_audit_event"};

        // Act + Assert
        for (String table : tables) {
            assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class));
        }
    }

    @Test
    void the_database_rejects_a_scoped_audience_without_a_reference() {
        // Arrange
        insertNotice();

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO institutional_notice_audience (notice_id, audience_kind, scope_reference) "
                                + "VALUES (?, 'SITE', '')", NOTICE_ID));
    }

    @Test
    void the_database_rejects_a_university_audience_that_points_at_one_scope() {
        // Arrange
        insertNotice();

        // Act + Assert
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO institutional_notice_audience (notice_id, audience_kind, scope_reference) "
                                + "VALUES (?, 'UNIVERSITY', 'TUNJA')", NOTICE_ID));
    }

    @Test
    void the_database_rejects_an_inverted_availability_window() {
        // Arrange + Act + Assert
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
                INSERT INTO institutional_notice
                    (notice_id, title, body, source_reference, published_from, published_through, published_by, published_at)
                VALUES (?, 'title', 'body', 'reference', ?, ?, 'actor', ?)
                """, NOTICE_ID, java.time.LocalDate.parse("2026-10-10"), java.time.LocalDate.parse("2026-10-03"),
                Timestamp.from(java.time.Instant.parse("2026-10-02T15:00:00Z"))));
    }

    private void insertNotice() {
        jdbcTemplate.update("""
                INSERT INTO institutional_notice
                    (notice_id, title, body, source_reference, published_from, published_through, published_by, published_at)
                VALUES (?, 'Convocatoria de prueba', 'Cuerpo del aviso', 'Resolución 1 de 2026', ?, ?, 'actor', ?)
                """, NOTICE_ID, java.time.LocalDate.parse("2026-10-03"), java.time.LocalDate.parse("2026-10-10"),
                Timestamp.from(java.time.Instant.parse("2026-10-02T15:00:00Z")));
    }
}