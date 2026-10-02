package co.edu.uptc.universiry.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record RegisteredIdentity(UUID id, UUID userId, AuthenticatedPrincipal principal, Instant firstSeenAt) {

    public RegisteredIdentity {
        if (id == null || userId == null || principal == null || firstSeenAt == null) {
            throw new IllegalArgumentException("registered identity is missing required values");
        }
    }
}
