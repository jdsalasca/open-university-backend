package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.RoleAssignment;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record CurrentRoleAssignmentResponse(
        String profileKey,
        List<AssignmentScope> scopes,
        LocalDate validFrom,
        LocalDate validThrough) {

    static CurrentRoleAssignmentResponse from(RoleAssignment assignment) {
        return new CurrentRoleAssignmentResponse(
                assignment.profile().key(),
                assignment.scopes().stream().sorted(Comparator.comparing(scope -> scope.kind().name())).toList(),
                assignment.validFrom(), assignment.validThrough());
    }
}
