package co.edu.uptc.universiry.admissions.domain;

import java.time.LocalDate;

public record AdmissionsMilestone(
        String key,
        AdmissionsMilestoneKind kind,
        LocalDate startsOn,
        LocalDate endsOn,
        String title,
        String description
) {
    public AdmissionsMilestone {
        key = AdmissionsSource.requiredText(key, 64, "milestone.key");
        if (!key.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("milestone.key must be a lowercase stable key");
        }
        if (kind == null || startsOn == null || endsOn == null || endsOn.isBefore(startsOn)) {
            throw new IllegalArgumentException("milestone kind and ordered inclusive dates are required");
        }
        title = AdmissionsSource.requiredText(title, 160, "milestone.title");
        description = AdmissionsSource.requiredText(description, 500, "milestone.description");
    }
}
