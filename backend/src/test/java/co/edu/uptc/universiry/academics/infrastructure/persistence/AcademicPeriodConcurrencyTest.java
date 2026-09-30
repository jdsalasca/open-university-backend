package co.edu.uptc.universiry.academics.infrastructure.persistence;

import co.edu.uptc.universiry.academics.application.AcademicCalendarDraftCommand;
import co.edu.uptc.universiry.academics.application.AcademicPeriodConflictException;
import co.edu.uptc.universiry.academics.application.AcademicPeriodCreateCommand;
import co.edu.uptc.universiry.academics.application.AcademicPeriodRepository;
import co.edu.uptc.universiry.academics.application.AcademicPeriodService;
import co.edu.uptc.universiry.academics.domain.AcademicCalendarActivity;
import co.edu.uptc.universiry.academics.domain.AcademicPeriod;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:academic-period-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
class AcademicPeriodConcurrencyTest {

    @Autowired
    private AcademicPeriodService service;

    @Autowired
    private AcademicPeriodRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrent_open_commands_from_the_same_approved_snapshot_create_one_transition_and_one_audit_event()
            throws Exception {
        // Arrange
        String setupActor = "period.setup";
        String code = "CONCURRENT-" + UUID.randomUUID().toString().substring(0, 8);
        var period = service.createPeriod(new AcademicPeriodCreateCommand(code, AcademicPeriodKind.REGULAR,
                2027, 1, LocalDate.of(2027, 1, 15), LocalDate.of(2027, 6, 30)), setupActor).period();
        var calendar = service.createCalendar(period.id(), new AcademicCalendarDraftCommand(
                "Resolución sintética 1 de 2027",
                java.util.List.of(new AcademicCalendarActivity("REGISTRATION", "Inscripción",
                        LocalDateTime.of(2027, 1, 10, 8, 0), LocalDateTime.of(2027, 1, 12, 17, 0)))), setupActor);
        var published = service.publishCalendar(period.id(), calendar.id(), setupActor);
        var approved = service.approve(period.id(), published.id(), "Acuerdo sintético 1 de 2027", setupActor);
        AcademicPeriod sameApprovedSnapshot = repository.findPeriod(approved.id()).orElseThrow();
        AcademicPeriod firstOpening = sameApprovedSnapshot.open("period.operator.one", Instant.parse("2026-09-30T20:00:00Z"));
        AcademicPeriod secondOpening = sameApprovedSnapshot.open("period.operator.two", Instant.parse("2026-09-30T20:00:00Z"));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Act
        try {
            Future<Boolean> first = executor.submit(() -> transitionAfter(start, firstOpening, "period.operator.one"));
            Future<Boolean> second = executor.submit(() -> transitionAfter(start, secondOpening, "period.operator.two"));
            start.countDown();

            // Assert
            assertTrue(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS));
            assertEquals(AcademicPeriodStatus.OPEN, repository.findPeriod(approved.id()).orElseThrow().status());
            assertEquals(1, countOpeningEvents(approved.id()));
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean transitionAfter(CountDownLatch start, AcademicPeriod opened, String actor) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent start timed out.");
        try {
            repository.transition(opened, AcademicPeriodStatus.APPROVED, "PERIOD_OPENED", actor, null);
            return true;
        } catch (AcademicPeriodConflictException conflict) {
            return false;
        }
    }

    private int countOpeningEvents(UUID periodId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM academic_period_audit_event
                WHERE period_id = ? AND action_key = 'PERIOD_OPENED'
                """, Integer.class, periodId.toString());
    }
}
