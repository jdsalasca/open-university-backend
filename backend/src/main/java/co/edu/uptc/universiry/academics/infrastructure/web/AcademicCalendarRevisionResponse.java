package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.domain.AcademicCalendarActivity;
import co.edu.uptc.universiry.academics.domain.AcademicCalendarRevision;
import co.edu.uptc.universiry.academics.domain.AcademicCalendarStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AcademicCalendarRevisionResponse(
        UUID id,
        UUID periodId,
        int version,
        String officialReference,
        AcademicCalendarStatus status,
        List<ActivityResponse> activities
) {
    static AcademicCalendarRevisionResponse from(AcademicCalendarRevision revision) {
        return new AcademicCalendarRevisionResponse(revision.id(), revision.periodId(), revision.version(),
                revision.officialReference(), revision.status(),
                revision.activities().stream().map(ActivityResponse::from).toList());
    }

    public record ActivityResponse(
            String key,
            String label,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            UUID organizationUnitId,
            UUID siteId
    ) {
        static ActivityResponse from(AcademicCalendarActivity activity) {
            return new ActivityResponse(activity.key(), activity.label(), activity.startsAt(), activity.endsAt(),
                    activity.organizationUnitId(), activity.siteId());
        }
    }
}
