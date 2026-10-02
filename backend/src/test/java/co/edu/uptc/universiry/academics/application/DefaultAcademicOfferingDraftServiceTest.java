package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOfferingDraft;
import co.edu.uptc.universiry.academics.domain.AcademicOfferingVersionConflictException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAcademicOfferingDraftServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void creates_a_normalized_draft_and_records_actor_time_and_reference() {
        // Arrange
        RecordingRepository repository = new RecordingRepository();
        DefaultAcademicOfferingDraftService service = new DefaultAcademicOfferingDraftService(repository, CLOCK);
        UUID periodId = UUID.randomUUID();
        UUID curriculumId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        var command = new AcademicOfferingDraftCommand(periodId, curriculumId, subjectId,
                "g-01", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 15), 28,
                "  Acta de programación 14  ");

        // Act
        AcademicOfferingDraft created = service.create(command, "academic.operator");

        // Assert
        assertEquals("G-01", created.sectionCode());
        assertEquals(periodId, created.periodId());
        assertEquals(curriculumId, created.curriculumId());
        assertEquals(subjectId, created.subjectId());
        assertEquals(1, created.version());
        assertEquals("Acta de programación 14", repository.sourceReference);
        assertEquals("academic.operator", repository.actorSub);
        assertEquals(NOW, repository.occurredAt);
        assertEquals(created, repository.saved);
    }

    @Test
    void invalid_reference_is_rejected_before_audit_or_persistence() {
        // Arrange
        RecordingRepository repository = new RecordingRepository();
        DefaultAcademicOfferingDraftService service = new DefaultAcademicOfferingDraftService(repository, CLOCK);
        var command = new AcademicOfferingDraftCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "G-01", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 15), 28, "  ");

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> service.create(command, "academic.operator"));
        assertEquals(null, repository.saved);
    }

    @Test
    void stale_updates_are_rejected_before_repository_mutation() {
        // Arrange
        RecordingRepository repository = new RecordingRepository();
        UUID offeringId = UUID.randomUUID();
        repository.current = AcademicOfferingDraft.create(offeringId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "G-01", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 3, 15), 28);
        DefaultAcademicOfferingDraftService service = new DefaultAcademicOfferingDraftService(repository, CLOCK);
        var command = new AcademicOfferingDraftUpdateCommand(2, "G-02", LocalDate.of(2027, 1, 16),
                LocalDate.of(2027, 3, 15), 24, "Acta de programación 15");

        // Act + Assert
        assertThrows(AcademicOfferingVersionConflictException.class,
                () -> service.update(offeringId, command, "academic.operator"));
        assertEquals(null, repository.updated);
    }

    private static final class RecordingRepository implements AcademicOfferingDraftRepository {
        private AcademicOfferingDraft saved;
        private AcademicOfferingDraft current;
        private AcademicOfferingDraft updated;
        private String sourceReference;
        private String actorSub;
        private Instant occurredAt;

        @Override
        public void create(AcademicOfferingDraft draft, String reference, String actor, Instant at) {
            saved = draft;
            sourceReference = reference;
            actorSub = actor;
            occurredAt = at;
        }

        @Override
        public void update(AcademicOfferingDraft draft, int expectedVersion, String reference,
                           String actor, Instant at) {
            updated = draft;
        }

        @Override
        public Optional<AcademicOfferingDraft> find(UUID id) {
            return Optional.ofNullable(current).filter(draft -> draft.id().equals(id));
        }

        @Override
        public AcademicOfferingDraftPage findByPeriod(UUID periodId, int limit, AcademicOfferingDraftCursor before) {
            return new AcademicOfferingDraftPage(java.util.List.of(), null);
        }

        @Override
        public AcademicOfferingAuditPage findAuditEvents(UUID offeringId, int limit, Long beforeId) {
            return new AcademicOfferingAuditPage(java.util.List.of(), null);
        }
    }
}
