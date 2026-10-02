package co.edu.uptc.universiry.admissions.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public record AdmissionsCallContent(
        String title,
        String callName,
        LocalDate updatedAt,
        LocalDate checkedAt,
        AdmissionsSource source,
        AdmissionsSource confirmationSource,
        List<AdmissionsMilestone> milestones
) {
    public AdmissionsCallContent {
        title = AdmissionsSource.requiredText(title, 160, "title");
        callName = AdmissionsSource.requiredText(callName, 160, "callName");
        if (updatedAt == null || checkedAt == null || updatedAt.isAfter(checkedAt)) {
            throw new IllegalArgumentException("updatedAt and checkedAt must be present and ordered");
        }
        if (source == null || confirmationSource == null) {
            throw new IllegalArgumentException("both source references are required");
        }
        List<AdmissionsMilestone> copied = milestones == null ? List.of() : List.copyOf(milestones);
        if (copied.size() > 50 || new HashSet<>(copied.stream().map(AdmissionsMilestone::key).toList()).size() != copied.size()) {
            throw new IllegalArgumentException("milestones must have unique keys and contain at most 50 entries");
        }
        milestones = copied.stream()
                .sorted(Comparator.comparing(AdmissionsMilestone::startsOn).thenComparing(AdmissionsMilestone::key))
                .toList();
    }

    public void requirePublishable() {
        if (milestones.isEmpty()) {
            throw new IllegalArgumentException("a published admissions call requires at least one milestone");
        }
    }
}
