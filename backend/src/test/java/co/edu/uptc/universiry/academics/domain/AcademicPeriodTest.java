package co.edu.uptc.universiry.academics.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcademicPeriodTest {

    private static final LocalDate START = LocalDate.of(2027, 1, 15);
    private static final LocalDate END = LocalDate.of(2027, 6, 30);
    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    void regular_period_requires_published_calendar_before_it_can_be_opened() {
        // Arrange
        AcademicPeriod period = period(AcademicPeriodKind.REGULAR);
        AcademicCalendarRevision calendar = calendar(period, AcademicCalendarStatus.DRAFT);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> period.approve(calendar, "Acuerdo de aprobación", "operator", NOW));
    }

    @Test
    void approved_period_can_be_opened_and_closed_in_order() {
        // Arrange
        AcademicPeriod draft = period(AcademicPeriodKind.REGULAR);
        AcademicCalendarRevision calendar = calendar(draft, AcademicCalendarStatus.PUBLISHED);

        // Act
        AcademicPeriod approved = draft.approve(calendar, "Acuerdo de aprobación", "operator", NOW);
        AcademicPeriod opened = approved.open("operator", NOW.plusSeconds(1));
        AcademicPeriod closed = opened.close("operator", NOW.plusSeconds(2));

        // Assert
        assertEquals(AcademicPeriodStatus.APPROVED, approved.status());
        assertEquals(AcademicPeriodStatus.OPEN, opened.status());
        assertEquals(AcademicPeriodStatus.CLOSED, closed.status());
        assertEquals(calendar.id(), closed.approvedCalendarRevisionId());
    }

    @Test
    void intersemester_is_a_distinct_period_kind_with_the_same_auditable_lifecycle() {
        // Arrange
        AcademicPeriod draft = period(AcademicPeriodKind.INTERSEMESTRAL);
        AcademicCalendarRevision calendar = calendar(draft, AcademicCalendarStatus.PUBLISHED);

        // Act
        AcademicPeriod opened = draft.approve(calendar, "Acuerdo de aprobación", "operator", NOW)
                .open("operator", NOW.plusSeconds(1));

        // Assert
        assertEquals(AcademicPeriodKind.INTERSEMESTRAL, opened.kind());
        assertEquals(AcademicPeriodStatus.OPEN, opened.status());
    }

    @Test
    void calendar_may_include_registration_before_instructional_period_start() {
        // Arrange
        AcademicPeriod period = period(AcademicPeriodKind.REGULAR);
        AcademicCalendarRevision calendar = AcademicCalendarRevision.draft(
                UUID.randomUUID(), period.id(), 1, "Resolución 123 de 2026",
                List.of(new AcademicCalendarActivity("REGISTRATION", "Inscripción de asignaturas",
                        LocalDateTime.of(2027, 1, 10, 8, 0), LocalDateTime.of(2027, 1, 12, 17, 0))));

        // Act
        AcademicCalendarRevision published = calendar.publish(period, "calendar.publisher", NOW);

        // Assert
        assertEquals(AcademicCalendarStatus.PUBLISHED, published.status());
        assertEquals(LocalDateTime.of(2027, 1, 10, 8, 0), published.activities().getFirst().startsAt());
    }

    @Test
    void published_calendar_cannot_be_published_twice() {
        // Arrange
        AcademicPeriod period = period(AcademicPeriodKind.REGULAR);
        AcademicCalendarRevision published = calendar(period, AcademicCalendarStatus.PUBLISHED);

        // Act + Assert
        assertThrows(AcademicPeriodStateConflictException.class,
                () -> published.publish(period, "calendar.publisher", NOW.plusSeconds(1)));
    }

    @Test
    void rejects_opening_before_approval_and_duplicate_transitions() {
        // Arrange
        AcademicPeriod draft = period(AcademicPeriodKind.REGULAR);

        // Act + Assert
        assertThrows(AcademicPeriodStateConflictException.class, () -> draft.open("operator", NOW));
        AcademicPeriod approved = draft.approve(
                calendar(draft, AcademicCalendarStatus.PUBLISHED), "Acuerdo de aprobación", "operator", NOW);
        AcademicPeriod opened = approved.open("operator", NOW.plusSeconds(1));
        assertThrows(AcademicPeriodStateConflictException.class, () -> opened.open("operator", NOW.plusSeconds(2)));
        assertThrows(AcademicPeriodStateConflictException.class, () -> approved.close("operator", NOW.plusSeconds(2)));
    }

    @Test
    void draft_or_approved_period_can_be_cancelled_but_open_period_cannot() {
        // Arrange
        AcademicPeriod draft = period(AcademicPeriodKind.REGULAR);
        AcademicPeriod cancelled = draft.cancel("operator", NOW);
        AcademicPeriod approvalDraft = period(AcademicPeriodKind.REGULAR);
        AcademicPeriod approved = approvalDraft.approve(
                calendar(approvalDraft, AcademicCalendarStatus.PUBLISHED), "Acuerdo de aprobación", "operator", NOW);

        // Act + Assert
        assertEquals(AcademicPeriodStatus.CANCELLED, cancelled.status());
        assertEquals(AcademicPeriodStatus.CANCELLED, approved.cancel("operator", NOW.plusSeconds(1)).status());
        AcademicPeriod opened = approved.open("operator", NOW.plusSeconds(1));
        assertThrows(AcademicPeriodStateConflictException.class, () -> opened.cancel("operator", NOW.plusSeconds(2)));
    }

    @Test
    void open_period_can_select_a_published_calendar_amendment_without_changing_its_state() {
        // Arrange
        AcademicPeriod draft = period(AcademicPeriodKind.REGULAR);
        AcademicCalendarRevision original = calendar(draft, AcademicCalendarStatus.PUBLISHED);
        AcademicPeriod open = draft.approve(original, "Acuerdo de aprobación", "operator", NOW)
                .open("operator", NOW.plusSeconds(1));
        AcademicCalendarRevision amendment = AcademicCalendarRevision.draft(
                UUID.randomUUID(), draft.id(), 2, "Resolución modificatoria 456 de 2027",
                List.of(new AcademicCalendarActivity("REGISTRATION", "Inscripciones ajustadas",
                        LocalDateTime.of(2027, 1, 18, 8, 0), LocalDateTime.of(2027, 1, 22, 17, 0))))
                .publish(draft, "calendar.publisher", NOW.plusSeconds(2));

        // Act
        AcademicPeriod updated = open.selectCalendarRevision(original, amendment);

        // Assert
        assertEquals(AcademicPeriodStatus.OPEN, updated.status());
        assertEquals(amendment.id(), updated.approvedCalendarRevisionId());
        assertEquals(open.openedAt(), updated.openedAt());
        assertThrows(IllegalArgumentException.class, () -> open.selectCalendarRevision(original,
                calendar(period(AcademicPeriodKind.REGULAR), AcademicCalendarStatus.PUBLISHED)));
        assertThrows(AcademicPeriodStateConflictException.class,
                () -> open.close("operator", NOW.plusSeconds(3)).selectCalendarRevision(amendment, amendment));
    }

    private static AcademicPeriod period(AcademicPeriodKind kind) {
        UUID id = UUID.randomUUID();
        return AcademicPeriod.create(id, "2027-I", kind, 2027, 1, START, END);
    }

    private static AcademicCalendarRevision calendar(AcademicPeriod period, AcademicCalendarStatus status) {
        AcademicCalendarRevision draft = AcademicCalendarRevision.draft(
                UUID.randomUUID(), period.id(), 1, "Resolución 123 de 2026",
                List.of(new AcademicCalendarActivity("REGISTRATION", "Inscripciones",
                        LocalDateTime.of(2027, 1, 16, 8, 0), LocalDateTime.of(2027, 1, 20, 17, 0))));
        return status == AcademicCalendarStatus.PUBLISHED
                ? draft.publish(period, "calendar.publisher", NOW)
                : draft;
    }
}
