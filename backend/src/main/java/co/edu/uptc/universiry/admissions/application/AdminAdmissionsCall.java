package co.edu.uptc.universiry.admissions.application;

import co.edu.uptc.universiry.admissions.domain.AdmissionsCallRevision;

import java.util.UUID;

public record AdminAdmissionsCall(
        UUID id,
        String callKey,
        UUID currentPublishedRevisionId,
        AdmissionsCallRevision latestRevision,
        AdmissionsCallRevision publishedRevision
) {
}
