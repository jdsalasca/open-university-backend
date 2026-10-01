package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.domain.RoleProfile;
import co.edu.uptc.universiry.identity.domain.ScopeKind;

import java.util.Arrays;
import java.util.List;

public record RoleProfileResponse(
        String key,
        String displayName,
        boolean manuallyAssignable,
        List<ScopeKind> allowedScopeKinds,
        List<String> permissions) {

    static RoleProfileResponse from(RoleProfile profile) {
        List<ScopeKind> scopes = Arrays.stream(ScopeKind.values())
                .filter(profile.allowedScopeKinds()::contains)
                .toList();
        List<String> permissions = profile.permissions().stream()
                .map(permission -> permission.authority())
                .sorted()
                .toList();
        return new RoleProfileResponse(profile.key(), profile.displayName(), profile.manuallyAssignable(),
                scopes, permissions);
    }
}
