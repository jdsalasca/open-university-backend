package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCalendarDraftCommand;
import co.edu.uptc.universiry.academics.domain.AcademicCalendarActivity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAcademicCalendarRequest(
        String officialReference,
        @Size(max = 200) List<@Valid ActivityRequest> activities
) {
    AcademicCalendarDraftCommand toCommand() {
        List<AcademicCalendarActivity> domainActivities = activities == null ? List.of()
                : activities.stream().map(ActivityRequest::toDomain).toList();
        return new AcademicCalendarDraftCommand(officialReference, domainActivities);
    }

    public record ActivityRequest(
            @NotBlank String key,
            @NotBlank String label,
            @NotNull LocalDateTime startsAt,
            @NotNull LocalDateTime endsAt,
            UUID organizationUnitId,
            UUID siteId
    ) {
        AcademicCalendarActivity toDomain() {
            return new AcademicCalendarActivity(key, label, startsAt, endsAt, organizationUnitId, siteId);
        }
    }
}
