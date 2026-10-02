package co.edu.uptc.universiry.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record AccessAuditEvent(
        UUID id,
        UUID assignmentId,
        AccessAuditAction action,
        UUID actorIdentityId,
        UUID actorUserId,
        Instant occurredAt,
        InstitutionalReference sourceReference,
        long previousVersion,
        long version) {

    public AccessAuditEvent {
        if (id == null || assignmentId == null || action == null || actorIdentityId == null
                || actorUserId == null || occurredAt == null
                || sourceReference == null || previousVersion < 0
                || version != previousVersion + 1) {
            throw new IllegalArgumentException("audit event must describe a single valid assignment transition");
        }
        if (action == AccessAuditAction.GRANTED && (previousVersion != 0 || version != 1)) {
            throw new IllegalArgumentException("grant audit must describe the initial assignment version");
        }
        if (action == AccessAuditAction.REVOKED && previousVersion < 1) {
            throw new IllegalArgumentException("revocation audit requires an existing assignment");
        }
    }

}
