package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.application.PublicAdmissionsCall;

import java.time.Instant;
import java.util.UUID;

public record PublicAdmissionsCallResponse(
        UUID callId,
        String callKey,
        UUID revisionId,
        int revisionNumber,
        AdmissionsCallContentResponse content,
        String officialReference,
        Instant publishedAt
) {
    public static PublicAdmissionsCallResponse from(PublicAdmissionsCall call) {
        return new PublicAdmissionsCallResponse(call.callId(), call.callKey(), call.revisionId(),
                call.revisionNumber(), AdmissionsCallContentResponse.from(call.content()), call.officialReference(),
                call.publishedAt());
    }
}
