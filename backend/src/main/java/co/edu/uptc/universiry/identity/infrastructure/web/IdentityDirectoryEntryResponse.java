package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.domain.RegisteredIdentity;
import java.util.UUID;

public record IdentityDirectoryEntryResponse(UUID userId, String issuer, String subject) {

    static IdentityDirectoryEntryResponse from(RegisteredIdentity identity) {
        return new IdentityDirectoryEntryResponse(
                identity.userId(), identity.principal().issuer(), identity.principal().subject());
    }
}
