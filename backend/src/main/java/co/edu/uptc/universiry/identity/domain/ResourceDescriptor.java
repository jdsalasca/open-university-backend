package co.edu.uptc.universiry.identity.domain;

import java.util.Map;

public record ResourceDescriptor(Map<ScopeKind, String> scopeReferences) {

    public ResourceDescriptor {
        if (scopeReferences == null) {
            throw new IllegalArgumentException("resource scope references are required");
        }
        scopeReferences.forEach((kind, reference) -> {
            if (kind == null || kind == ScopeKind.UNIVERSITY) {
                throw new IllegalArgumentException("resource references must use a concrete scope kind");
            }
            new AssignmentScope(kind, reference);
        });
        scopeReferences = Map.copyOf(scopeReferences);
    }

    public boolean matches(AssignmentScope scope) {
        if (scope.kind() == ScopeKind.UNIVERSITY) {
            return true;
        }
        return scope.stableReference().equals(scopeReferences.get(scope.kind()));
    }
}
