package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.application.AdminAdmissionsCall;

import java.util.UUID;

public record AdmissionsCallAdminResponse(
        UUID id,
        String callKey,
        UUID currentPublishedRevisionId,
        AdmissionsCallRevisionResponse latestRevision,
        AdmissionsCallRevisionResponse publishedRevision
) {
    public static AdmissionsCallAdminResponse from(AdminAdmissionsCall call) {
        return new AdmissionsCallAdminResponse(call.id(), call.callKey(), call.currentPublishedRevisionId(),
                call.latestRevision() == null ? null : AdmissionsCallRevisionResponse.from(call.latestRevision()),
                call.publishedRevision() == null ? null : AdmissionsCallRevisionResponse.from(call.publishedRevision()));
    }
}
