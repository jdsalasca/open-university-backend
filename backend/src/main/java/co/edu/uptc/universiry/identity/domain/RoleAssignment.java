package co.edu.uptc.universiry.identity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record RoleAssignment(
        UUID id,
        UUID targetUserId,
        RoleProfile profile,
        Set<AssignmentScope> scopes,
        LocalDate validFrom,
        LocalDate validThrough,
        AssignmentStatus status,
        UUID grantedByUserId,
        InstitutionalReference sourceReference,
        Instant createdAt,
        long version) {

    public RoleAssignment {
        if (id == null || targetUserId == null || profile == null || validFrom == null || status == null
                || grantedByUserId == null || sourceReference == null || createdAt == null || version < 1) {
            throw new IllegalArgumentException("role assignment is missing required values");
        }
        if (targetUserId.equals(grantedByUserId)) {
            throw new IllegalArgumentException("a user cannot grant a role to itself");
        }
        if (validThrough != null && validThrough.isBefore(validFrom)) {
            throw new IllegalArgumentException("role assignment end date cannot precede its start date");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("at least one scope is required");
        }
        scopes = Set.copyOf(scopes);
        if (scopes.stream().map(AssignmentScope::kind).distinct().count() != scopes.size()) {
            throw new IllegalArgumentException("a role assignment cannot repeat a scope kind");
        }
        if (!profile.manuallyAssignable()) {
            throw new IllegalArgumentException("lifecycle profiles are derived from their verified source");
        }
        if (scopes.stream().anyMatch(scope -> !profile.allowedScopeKinds().contains(scope.kind()))) {
            throw new IllegalArgumentException("role profile does not allow one or more supplied scope kinds");
        }
        if (profile == RoleProfile.ADMINISTRATOR && !scopes.equals(Set.of(new AssignmentScope(ScopeKind.UNIVERSITY, null)))) {
            throw new IllegalArgumentException("administrator role requires exactly the university scope");
        }
    }

    public boolean matches(ResourceDescriptor resource, LocalDate institutionalDate) {
        if (resource == null || !isActiveOn(institutionalDate)) {
            return false;
        }
        return scopes.stream().allMatch(resource::matches);
    }

    public boolean isActiveOn(LocalDate institutionalDate) {
        return institutionalDate != null && status == AssignmentStatus.ACTIVE
                && !institutionalDate.isBefore(validFrom)
                && (validThrough == null || !institutionalDate.isAfter(validThrough));
    }

}
