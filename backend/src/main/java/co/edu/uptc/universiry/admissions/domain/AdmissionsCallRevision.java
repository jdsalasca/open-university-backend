package co.edu.uptc.universiry.admissions.domain;

import java.time.Instant;
import java.util.UUID;

public record AdmissionsCallRevision(
        UUID id,
        UUID callId,
        int revisionNumber,
        int draftVersion,
        AdmissionsCallStatus status,
        AdmissionsCallContent content,
        UUID createdByUserId,
        UUID createdByIdentityId,
        Instant createdAt,
        UUID publishedByUserId,
        UUID publishedByIdentityId,
        Instant publishedAt,
        String officialReference
) {
    public AdmissionsCallRevision {
        if (id == null || callId == null || revisionNumber < 1 || draftVersion < 1 || status == null || content == null
                || createdByUserId == null || createdByIdentityId == null || createdAt == null) {
            throw new IllegalArgumentException("admissions revision is missing required values");
        }
        boolean published = status == AdmissionsCallStatus.PUBLISHED;
        boolean anyPublicationMetadata = publishedByUserId != null || publishedByIdentityId != null
                || publishedAt != null || officialReference != null;
        boolean completePublicationMetadata = publishedByUserId != null && publishedByIdentityId != null
                && publishedAt != null && officialReference != null && !officialReference.isBlank();
        if (published != completePublicationMetadata || anyPublicationMetadata != completePublicationMetadata) {
            throw new IllegalArgumentException("publication metadata must match the revision state");
        }
    }

    public AdmissionsCallRevision revise(AdmissionsCallContent newContent) {
        if (status != AdmissionsCallStatus.DRAFT || newContent == null) {
            throw new IllegalArgumentException("only a draft revision can be edited");
        }
        return new AdmissionsCallRevision(id, callId, revisionNumber, Math.addExact(draftVersion, 1), status,
                newContent, createdByUserId, createdByIdentityId, createdAt, null, null, null, null);
    }

    public AdmissionsCallRevision publish(AdmissionsActor actor, Instant at, String reference) {
        if (status != AdmissionsCallStatus.DRAFT) {
            throw new IllegalArgumentException("only a draft revision can be published");
        }
        if (actor == null || at == null || reference == null || reference.isBlank() || reference.trim().length() > 240) {
            throw new IllegalArgumentException("publication actor, time and official reference are required");
        }
        content.requirePublishable();
        return new AdmissionsCallRevision(id, callId, revisionNumber, draftVersion, AdmissionsCallStatus.PUBLISHED,
                content, createdByUserId, createdByIdentityId, createdAt, actor.userId(), actor.identityId(), at,
                reference.trim());
    }
}
