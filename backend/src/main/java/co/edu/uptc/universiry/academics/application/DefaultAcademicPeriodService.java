package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCalendarRevision;
import co.edu.uptc.universiry.academics.domain.AcademicPeriod;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultAcademicPeriodService implements AcademicPeriodService {

    private final AcademicPeriodRepository repository;
    private final Clock clock;

    public DefaultAcademicPeriodService(AcademicPeriodRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<AcademicPeriodView> publicPeriods() {
        return repository.findPublicPeriods();
    }

    @Override
    public List<AcademicPeriodView> adminPeriods() {
        return repository.findAdminPeriods();
    }

    @Override
    public AcademicPeriodHistoryView history(UUID periodId) {
        AcademicPeriodView period = repository.findView(periodId)
                .orElseThrow(AcademicPeriodNotFoundException::new);
        return new AcademicPeriodHistoryView(period,
                repository.findCalendarHistory(periodId), repository.findAuditHistory(periodId));
    }

    @Override
    public AcademicPeriodView createPeriod(AcademicPeriodCreateCommand command, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicPeriod period = AcademicPeriod.create(UUID.randomUUID(), command.code(), command.kind(),
                command.academicYear(), command.sequenceNumber(), command.startsOn(), command.endsOn());
        repository.createPeriod(period, actor);
        return view(period.id());
    }

    @Override
    public AcademicCalendarRevision createCalendar(UUID periodId, AcademicCalendarDraftCommand command, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicPeriod period = requirePeriod(periodId);
        if (period.status() != AcademicPeriodStatus.DRAFT
                && period.status() != AcademicPeriodStatus.APPROVED
                && period.status() != AcademicPeriodStatus.OPEN) {
            throw new AcademicPeriodConflictException();
        }
        if (period.status() != AcademicPeriodStatus.DRAFT
                && (command.officialReference() == null || command.officialReference().isBlank())) {
            throw new IllegalArgumentException("calendar amendment requires an official reference");
        }
        AcademicCalendarRevision revision = AcademicCalendarRevision.draft(
                UUID.randomUUID(), periodId, repository.nextCalendarRevisionNumber(periodId),
                command.officialReference(), command.activities());
        repository.createCalendar(revision, actor);
        return revision;
    }

    @Override
    public AcademicCalendarRevision publishCalendar(UUID periodId, UUID revisionId, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicPeriod period = requirePeriod(periodId);
        AcademicCalendarRevision draft = requireCalendar(periodId, revisionId);
        AcademicCalendarRevision published = draft.publish(period, actor, clock.instant());
        repository.publishCalendar(published, actor);
        return published;
    }

    @Override
    public AcademicPeriodView activateCalendarRevision(UUID periodId, UUID revisionId, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicPeriod current = requirePeriod(periodId);
        if (current.approvedCalendarRevisionId() == null) throw new AcademicPeriodConflictException();
        AcademicCalendarRevision calendar = requireCalendar(periodId, revisionId);
        AcademicCalendarRevision activeCalendar = requireCalendar(periodId, current.approvedCalendarRevisionId());
        AcademicPeriod updated = current.selectCalendarRevision(activeCalendar, calendar);
        repository.selectCalendarRevision(updated, current.approvedCalendarRevisionId(), revisionId, actor,
                calendar.officialReference());
        return view(periodId);
    }

    @Override
    public AcademicPeriodView approve(UUID periodId, UUID revisionId, String approvalReference, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicPeriod current = requirePeriod(periodId);
        AcademicCalendarRevision calendar = requireCalendar(periodId, revisionId);
        AcademicPeriod approved = current.approve(calendar, approvalReference, actor, clock.instant());
        repository.transition(approved, AcademicPeriodStatus.DRAFT, "PERIOD_APPROVED", actor,
                approvalReference);
        return view(periodId);
    }

    @Override
    public AcademicPeriodView open(UUID periodId, String actorSub) {
        return transition(periodId, actorSub, AcademicPeriodStatus.APPROVED, "PERIOD_OPENED",
                period -> period.open(actorSub, clock.instant()));
    }

    @Override
    public AcademicPeriodView close(UUID periodId, String actorSub) {
        return transition(periodId, actorSub, AcademicPeriodStatus.OPEN, "PERIOD_CLOSED",
                period -> period.close(actorSub, clock.instant()));
    }

    @Override
    public AcademicPeriodView cancel(UUID periodId, String reference, String actorSub) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicPeriod current = requirePeriod(periodId);
        AcademicPeriod cancelled = current.cancel(actor, clock.instant());
        repository.transition(cancelled, current.status(), "PERIOD_CANCELLED", actor, reference);
        return view(periodId);
    }

    private AcademicPeriodView transition(
            UUID periodId, String actorSub, AcademicPeriodStatus expectedStatus, String action,
            java.util.function.Function<AcademicPeriod, AcademicPeriod> change
    ) {
        String actor = AcademicCatalogActorSub.require(actorSub);
        AcademicPeriod current = requirePeriod(periodId);
        AcademicPeriod updated = change.apply(current);
        repository.transition(updated, expectedStatus, action, actor, null);
        return view(periodId);
    }

    private AcademicPeriodView view(UUID periodId) {
        return repository.findView(periodId).orElseThrow(AcademicPeriodNotFoundException::new);
    }

    private AcademicPeriod requirePeriod(UUID periodId) {
        if (periodId == null) throw new AcademicPeriodNotFoundException();
        return repository.findPeriod(periodId).orElseThrow(AcademicPeriodNotFoundException::new);
    }

    private AcademicCalendarRevision requireCalendar(UUID periodId, UUID revisionId) {
        return repository.findCalendar(periodId, revisionId).orElseThrow(AcademicPeriodNotFoundException::new);
    }
}
