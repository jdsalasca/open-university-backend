package co.edu.uptc.universiry.admissions.application;

import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallRevision;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;

import java.util.List;
import java.util.UUID;

public interface AdmissionsCallService {
    List<PublicAdmissionsCall> publicCalls();

    List<AdminAdmissionsCall> adminCalls();

    AdminAdmissionsCall adminCall(UUID callId);

    AdminAdmissionsCall createCall(String callKey, AdmissionsCallContent content, AuthenticatedPrincipal principal);

    AdmissionsCallRevision createRevision(UUID callId, AdmissionsCallContent content,
                                          AuthenticatedPrincipal principal);

    AdmissionsCallRevision updateDraft(UUID callId, UUID revisionId, int expectedDraftVersion,
                                       AdmissionsCallContent content, AuthenticatedPrincipal principal);

    AdmissionsCallRevision publish(UUID callId, UUID revisionId, int expectedDraftVersion,
                                   UUID expectedPublishedRevisionId, String officialReference,
                                   AuthenticatedPrincipal principal);
}
