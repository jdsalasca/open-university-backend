package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicCalendarDraftCommand;
import co.edu.uptc.universiry.academics.application.AcademicPeriodConflictException;
import co.edu.uptc.universiry.academics.application.AcademicPeriodCreateCommand;
import co.edu.uptc.universiry.academics.application.AcademicPeriodRepository;
import co.edu.uptc.universiry.academics.application.AcademicPeriodService;
import co.edu.uptc.universiry.academics.domain.AcademicCalendarActivity;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfSystemProperty(named = "universiry.mysql-contract.enabled", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AcademicPeriodMySqlContractTest {

    @Autowired
    private AcademicPeriodService service;

    @Autowired
    private AcademicPeriodRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void mysqlDatasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_URL"));
        properties.add("spring.datasource.username", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_USERNAME"));
        properties.add("spring.datasource.password", () -> requiredEnvironment("UNIVERSIRY_MYSQL_TEST_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Test
    void stale_close_conflicts_without_reverting_the_active_calendar_revision() {
        // Arrange
        String actor = "synthetic-mysql-period-contract-test";
        String code = "MYSQL-PERIOD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        var draftPeriod = service.createPeriod(new AcademicPeriodCreateCommand(code, AcademicPeriodKind.REGULAR,
                2028, 1, LocalDate.of(2028, 1, 15), LocalDate.of(2028, 6, 30)), actor).period();
        var firstDraft = service.createCalendar(draftPeriod.id(), calendar("Resolución sintética 1 de 2028"), actor);
        var firstPublished = service.publishCalendar(draftPeriod.id(), firstDraft.id(), actor);
        var approved = service.approve(draftPeriod.id(), firstPublished.id(), "Acuerdo sintético 1 de 2028", actor);
        service.open(draftPeriod.id(), actor);
        var staleOpenSnapshot = repository.findPeriod(draftPeriod.id()).orElseThrow();

        var amendmentDraft = service.createCalendar(draftPeriod.id(),
                calendar("Resolución modificatoria sintética 2 de 2028"), actor);
        var amendment = service.publishCalendar(draftPeriod.id(), amendmentDraft.id(), actor);
        service.activateCalendarRevision(draftPeriod.id(), amendment.id(), actor);

        // Act
        assertThrows(AcademicPeriodConflictException.class, () -> repository.transition(
                staleOpenSnapshot.close("synthetic-period-operator", Instant.parse("2028-07-01T15:30:00Z")),
                AcademicPeriodStatus.OPEN, "PERIOD_CLOSED", "synthetic-period-operator", null));

        // Assert
        var current = repository.findPeriod(approved.period().id()).orElseThrow();
        assertEquals(AcademicPeriodStatus.OPEN, current.status());
        assertEquals(amendment.id(), current.approvedCalendarRevisionId());
        assertEquals(0, countClosingEvents(approved.period().id()));
    }

    private static AcademicCalendarDraftCommand calendar(String officialReference) {
        return new AcademicCalendarDraftCommand(officialReference, List.of(new AcademicCalendarActivity(
                "REGISTRATION", "Inscripción", LocalDateTime.of(2028, 1, 10, 8, 0),
                LocalDateTime.of(2028, 1, 12, 17, 0))));
    }

    private int countClosingEvents(UUID periodId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_period_audit_event
                WHERE period_id = ? AND action_key = 'PERIOD_CLOSED'
                """, Integer.class, periodId.toString());
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }
}
