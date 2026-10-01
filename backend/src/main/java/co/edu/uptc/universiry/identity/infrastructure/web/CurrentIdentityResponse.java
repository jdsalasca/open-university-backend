package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.application.CurrentIdentitySnapshot;
import java.util.List;

public record CurrentIdentityResponse(
        String subject,
        List<String> permissions,
        List<CurrentRoleAssignmentResponse> assignments) {

    static CurrentIdentityResponse from(CurrentIdentitySnapshot snapshot) {
        var roleAssignments = snapshot.assignments().stream()
                .map(CurrentRoleAssignmentResponse::from)
                .toList();
        return new CurrentIdentityResponse(snapshot.subject(), snapshot.permissions(), roleAssignments);
    }
}
