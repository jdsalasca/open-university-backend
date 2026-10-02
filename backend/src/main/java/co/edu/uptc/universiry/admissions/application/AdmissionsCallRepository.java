package co.edu.uptc.universiry.admissions.application;

import co.edu.uptc.universiry.admissions.domain.AdmissionsActor;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallRevision;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionsCallRepository {
    List<PublicAdmissionsCall> findPublicCalls();

    List<AdminAdmissionsCall> findAdminCalls();

    Optional<AdminAdmissionsCall> findAdminCall(UUID callId);

    void createCall(UUID callId, String callKey, UUID revisionId, AdmissionsCallContent content,
                    AdmissionsActor actor, Instant occurredAt);

    AdmissionsCallRevision createRevision(UUID callId, UUID revisionId, AdmissionsCallContent content,
                                          AdmissionsActor actor, Instant occurredAt);

    AdmissionsCallRevision updateDraft(UUID callId, UUID revisionId, int expectedDraftVersion,
                                       AdmissionsCallContent content, AdmissionsActor actor, Instant occurredAt);

    AdmissionsCallRevision publish(UUID callId, UUID revisionId, int expectedDraftVersion,
                                   UUID expectedPublishedRevisionId, String officialReference,
                                   AdmissionsActor actor, Instant occurredAt);
}
