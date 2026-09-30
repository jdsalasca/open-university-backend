package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicPeriodView;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodKind;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AcademicPeriodResponse(
        UUID id,
        String code,
        AcademicPeriodKind kind,
        int academicYear,
        int sequenceNumber,
        LocalDate startsOn,
        LocalDate endsOn,
        AcademicPeriodStatus status,
        UUID calendarRevisionId,
        Integer calendarRevisionNumber,
        String approvalReference,
        String officialReference,
        Instant createdAt
) {
    static AcademicPeriodResponse from(AcademicPeriodView view) {
        var period = view.period();
        return new AcademicPeriodResponse(period.id(), period.code(), period.kind(), period.academicYear(),
                period.sequenceNumber(), period.startsOn(), period.endsOn(), period.status(),
                period.approvedCalendarRevisionId(), view.calendarRevisionNumber(), period.approvalReference(),
                view.officialReference(),
                view.createdAt());
    }
}
