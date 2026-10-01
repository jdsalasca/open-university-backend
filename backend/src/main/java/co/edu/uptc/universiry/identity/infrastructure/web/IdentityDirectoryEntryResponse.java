package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.domain.RegisteredIdentity;

public record IdentityDirectoryEntryResponse(String issuer, String subject) {

    static IdentityDirectoryEntryResponse from(RegisteredIdentity identity) {
        return new IdentityDirectoryEntryResponse(identity.principal().issuer(), identity.principal().subject());
    }
}
