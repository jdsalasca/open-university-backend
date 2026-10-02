package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicOfferingDraft;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class DefaultAcademicOfferingDraftService implements AcademicOfferingDraftService {

    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_REFERENCE_LENGTH = 240;

    private final AcademicOfferingDraftRepository repository;
    private final Clock clock;

    public DefaultAcademicOfferingDraftService(AcademicOfferingDraftRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public AcademicOfferingDraftPage drafts(UUID periodId, int limit, String before) {
        if (periodId == null) throw new IllegalArgumentException("periodId is required");
        requireLimit(limit);
        return repository.findByPeriod(periodId, limit, AcademicOfferingDraftCursor.parse(before));
    }

    @Override
    public AcademicOfferingAuditPage history(UUID offeringId, int limit, String before) {
        if (offeringId == null) throw new IllegalArgumentException("offeringId is required");
        requireLimit(limit);
        if (repository.find(offeringId).isEmpty()) throw new AcademicOfferingDraftNotFoundException();
        AcademicOfferingAuditCursor cursor = AcademicOfferingAuditCursor.parse(before);
        return repository.findAuditEvents(offeringId, limit, cursor == null ? null : cursor.eventId());
    }

    @Override
    public AcademicOfferingDraft create(AcademicOfferingDraftCommand command, String actorSub) {
        if (command == null) throw new IllegalArgumentException("offering command is required");
        String actor = AcademicCatalogActorSub.require(actorSub);
        String reference = requireReference(command.sourceReference());
        AcademicOfferingDraft draft = AcademicOfferingDraft.create(UUID.randomUUID(), command.periodId(),
                command.curriculumId(), command.subjectId(), command.sectionCode(), command.startsOn(),
                command.endsOn(), command.proposedCapacity());
        repository.create(draft, reference, actor, clock.instant());
        return draft;
    }

    @Override
    public AcademicOfferingDraft update(
            UUID offeringId, AcademicOfferingDraftUpdateCommand command, String actorSub
    ) {
        if (offeringId == null || command == null) throw new IllegalArgumentException("offering update is required");
        String actor = AcademicCatalogActorSub.require(actorSub);
        String reference = requireReference(command.sourceReference());
        AcademicOfferingDraft current = repository.find(offeringId)
                .orElseThrow(AcademicOfferingDraftNotFoundException::new);
        AcademicOfferingDraft updated = current.revise(command.expectedVersion(), command.sectionCode(),
                command.startsOn(), command.endsOn(), command.proposedCapacity());
        repository.update(updated, command.expectedVersion(), reference, actor, clock.instant());
        return updated;
    }

    private static void requireLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) throw new IllegalArgumentException("limit is outside its range");
    }

    private static String requireReference(String reference) {
        if (reference == null) throw new IllegalArgumentException("sourceReference is required");
        String normalized = reference.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_REFERENCE_LENGTH
                || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("sourceReference is invalid");
        }
        return normalized;
    }
}
