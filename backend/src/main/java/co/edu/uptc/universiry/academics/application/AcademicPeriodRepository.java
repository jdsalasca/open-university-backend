package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCalendarRevision;
import co.edu.uptc.universiry.academics.domain.AcademicPeriod;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicPeriodRepository {
    List<AcademicPeriodView> findPublicPeriods();

    List<AcademicPeriodView> findAdminPeriods();

    Optional<AcademicPeriod> findPeriod(UUID periodId);

    Optional<AcademicPeriodView> findView(UUID periodId);

    Optional<AcademicCalendarRevision> findCalendar(UUID periodId, UUID revisionId);

    List<AcademicCalendarRevision> findCalendarHistory(UUID periodId);

    List<AcademicPeriodAuditEvent> findAuditHistory(UUID periodId);

    int nextCalendarRevisionNumber(UUID periodId);

    void createPeriod(AcademicPeriod period, String actorSub);

    void createCalendar(AcademicCalendarRevision revision, String actorSub);

    void publishCalendar(AcademicCalendarRevision revision, String actorSub);

    void selectCalendarRevision(AcademicPeriod updatedPeriod, UUID expectedRevisionId,
                                UUID selectedRevisionId, String actorSub, String officialReference);

    void transition(AcademicPeriod period, AcademicPeriodStatus expectedStatus, String actionKey,
                    String actorSub, String reference);
}
