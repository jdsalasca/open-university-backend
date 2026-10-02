package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.domain.AdmissionsCallRevision;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallStatus;

import java.time.Instant;
import java.util.UUID;

public record AdmissionsCallRevisionResponse(
        UUID id,
        int revisionNumber,
        int draftVersion,
        AdmissionsCallStatus status,
        AdmissionsCallContentResponse content,
        String officialReference,
        Instant publishedAt
) {
    public static AdmissionsCallRevisionResponse from(AdmissionsCallRevision revision) {
        return new AdmissionsCallRevisionResponse(revision.id(), revision.revisionNumber(), revision.draftVersion(),
                revision.status(), AdmissionsCallContentResponse.from(revision.content()),
                revision.officialReference(), revision.publishedAt());
    }
}
