package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.AssignmentScope;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CreateRoleAssignmentCommand(
        UUID targetUserId,
        String profileKey,
        Set<AssignmentScope> scopes,
        LocalDate validFrom,
        LocalDate validThrough,
        String sourceReference) {
}
