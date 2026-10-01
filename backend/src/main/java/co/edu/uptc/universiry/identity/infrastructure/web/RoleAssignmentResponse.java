package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record RoleAssignmentResponse(
        String assignmentId,
        String targetIssuer,
        String targetSubject,
        String profileKey,
        List<AssignmentScope> scopes,
        String status,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference,
        Instant createdAt,
        long version) {

    static RoleAssignmentResponse from(RoleAssignment assignment) {
        List<AssignmentScope> scopes = assignment.scopes().stream()
                .sorted(Comparator.comparing(scope -> scope.kind().name()))
                .toList();
        return new RoleAssignmentResponse(
                assignment.id().toString(), assignment.target().issuer(), assignment.target().subject(),
                assignment.profile().key(), scopes, assignment.status().name(), assignment.validFrom(),
                assignment.validThrough(), assignment.sourceReference().value(), assignment.createdAt(),
                assignment.version());
    }
}
