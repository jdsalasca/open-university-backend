package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import co.edu.uptc.universiry.identity.domain.AuthenticatedPrincipal;

import java.time.LocalDate;
import java.util.Set;

public record CreateRoleAssignmentCommand(
        AuthenticatedPrincipal target,
        String profileKey,
        Set<AssignmentScope> scopes,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference) {
}
