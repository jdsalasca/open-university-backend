package co.edu.uptc.universiry.admissions.application;

import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;

import java.time.Instant;
import java.util.UUID;

public record PublicAdmissionsCall(
        UUID callId,
        String callKey,
        UUID revisionId,
        int revisionNumber,
        AdmissionsCallContent content,
        String officialReference,
        Instant publishedAt
) {
}
