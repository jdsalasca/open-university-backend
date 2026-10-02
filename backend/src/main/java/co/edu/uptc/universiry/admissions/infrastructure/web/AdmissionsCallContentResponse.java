package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import co.edu.uptc.universiry.admissions.domain.AdmissionsMilestone;
import co.edu.uptc.universiry.admissions.domain.AdmissionsSource;

import java.time.LocalDate;
import java.util.List;

public record AdmissionsCallContentResponse(
        String title,
        String callName,
        LocalDate updatedAt,
        LocalDate checkedAt,
        AdmissionsSource source,
        AdmissionsSource confirmationSource,
        List<AdmissionsMilestone> milestones
) {
    public static AdmissionsCallContentResponse from(AdmissionsCallContent content) {
        return new AdmissionsCallContentResponse(content.title(), content.callName(), content.updatedAt(),
                content.checkedAt(), content.source(), content.confirmationSource(), content.milestones());
    }
}
