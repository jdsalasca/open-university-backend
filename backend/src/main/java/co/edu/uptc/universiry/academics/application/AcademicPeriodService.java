package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCalendarRevision;
import co.edu.uptc.universiry.academics.domain.AcademicPeriod;

import java.util.List;
import java.util.UUID;

public interface AcademicPeriodService {
    List<AcademicPeriodView> publicPeriods();

    List<AcademicPeriodView> adminPeriods();

    AcademicPeriodHistoryView history(UUID periodId);

    AcademicPeriodView createPeriod(AcademicPeriodCreateCommand command, String actorSub);

    AcademicCalendarRevision createCalendar(UUID periodId, AcademicCalendarDraftCommand command, String actorSub);

    AcademicCalendarRevision publishCalendar(UUID periodId, UUID revisionId, String actorSub);

    AcademicPeriodView activateCalendarRevision(UUID periodId, UUID revisionId, String actorSub);

    AcademicPeriodView approve(UUID periodId, UUID revisionId, String approvalReference, String actorSub);

    AcademicPeriodView open(UUID periodId, String actorSub);

    AcademicPeriodView close(UUID periodId, String actorSub);

    AcademicPeriodView cancel(UUID periodId, String reference, String actorSub);
}
