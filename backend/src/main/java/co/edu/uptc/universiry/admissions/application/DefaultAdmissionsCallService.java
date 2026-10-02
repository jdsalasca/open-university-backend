package co.edu.uptc.universiry.admissions.application;

import co.edu.uptc.universiry.admissions.domain.AdmissionsActor;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallContent;
import co.edu.uptc.universiry.admissions.domain.AdmissionsCallRevision;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultAdmissionsCallService implements AdmissionsCallService {

    private final AdmissionsCallRepository repository;
    private final AdmissionsActorResolver actors;
    private final Clock clock;

    public DefaultAdmissionsCallService(AdmissionsCallRepository repository, AdmissionsActorResolver actors, Clock clock) {
        this.repository = repository;
        this.actors = actors;
        this.clock = clock;
    }

    @Override
    public List<PublicAdmissionsCall> publicCalls() {
        return repository.findPublicCalls();
    }

    @Override
    public List<AdminAdmissionsCall> adminCalls() {
        return repository.findAdminCalls();
    }

    @Override
    public AdminAdmissionsCall adminCall(UUID callId) {
        return repository.findAdminCall(callId).orElseThrow(AdmissionsCallNotFoundException::new);
    }

    @Override
    public AdminAdmissionsCall createCall(String callKey, AdmissionsCallContent content,
                                          AuthenticatedPrincipal principal) {
        String normalizedKey = requireCallKey(callKey);
        AdmissionsActor actor = actors.resolve(principal);
        UUID callId = UUID.randomUUID();
        repository.createCall(callId, normalizedKey, UUID.randomUUID(), requireContent(content), actor, clock.instant());
        return adminCall(callId);
    }

    @Override
    public AdmissionsCallRevision createRevision(UUID callId, AdmissionsCallContent content,
                                                  AuthenticatedPrincipal principal) {
        if (callId == null) throw new IllegalArgumentException("callId is required");
        return repository.createRevision(callId, UUID.randomUUID(), requireContent(content), actors.resolve(principal),
                clock.instant());
    }

    @Override
    public AdmissionsCallRevision updateDraft(UUID callId, UUID revisionId, int expectedDraftVersion,
                                              AdmissionsCallContent content, AuthenticatedPrincipal principal) {
        if (callId == null || revisionId == null || expectedDraftVersion < 1) {
            throw new IllegalArgumentException("call, revision and expected draft version are required");
        }
        return repository.updateDraft(callId, revisionId, expectedDraftVersion, requireContent(content),
                actors.resolve(principal), clock.instant());
    }

    @Override
    public AdmissionsCallRevision publish(UUID callId, UUID revisionId, int expectedDraftVersion,
                                          UUID expectedPublishedRevisionId, String officialReference,
                                          AuthenticatedPrincipal principal) {
        if (callId == null || revisionId == null || expectedDraftVersion < 1) {
            throw new IllegalArgumentException("call, revision and expected draft version are required");
        }
        if (officialReference == null || officialReference.isBlank() || officialReference.trim().length() > 240
                || officialReference.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("a bounded official publication reference is required");
        }
        return repository.publish(callId, revisionId, expectedDraftVersion, expectedPublishedRevisionId,
                officialReference.trim(), actors.resolve(principal), clock.instant());
    }

    private static AdmissionsCallContent requireContent(AdmissionsCallContent content) {
        if (content == null) throw new IllegalArgumentException("admissions call content is required");
        return content;
    }

    private static String requireCallKey(String callKey) {
        if (callKey == null || callKey.length() > 64 || !callKey.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("callKey must be a lowercase stable key");
        }
        return callKey;
    }
}
