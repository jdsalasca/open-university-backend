package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.application.CreateRoleAssignmentCommand;
import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.ScopeKind;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateRoleAssignmentRequest(
        UUID targetUserId,
        String profileKey,
        List<ScopeRequest> scopes,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference) {

    public CreateRoleAssignmentCommand toCommand() {
        if (targetUserId == null) {
            throw new IllegalArgumentException("canonical role target user id is required");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("at least one role scope is required");
        }
        Set<ScopeKind> seenKinds = EnumSet.noneOf(ScopeKind.class);
        Set<AssignmentScope> resolvedScopes = new java.util.HashSet<>();
        for (ScopeRequest scope : scopes) {
            if (scope == null || scope.kind() == null || scope.kind().isBlank()) {
                throw new IllegalArgumentException("role scope kind is required");
            }
            ScopeKind kind = ScopeKind.valueOf(scope.kind());
            if (!seenKinds.add(kind)) {
                throw new IllegalArgumentException("a role assignment cannot repeat a scope kind");
            }
            resolvedScopes.add(new AssignmentScope(kind, scope.reference()));
        }
        return new CreateRoleAssignmentCommand(
                targetUserId, profileKey, Set.copyOf(resolvedScopes), validFrom, validThrough, sourceReference);
    }

    public record ScopeRequest(String kind, String reference) {
    }
}
