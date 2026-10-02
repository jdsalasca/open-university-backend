package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.RoleAssignment;

import java.util.List;
import java.util.UUID;

public record CurrentIdentitySnapshot(
        UUID userId,
        String subject,
        List<String> permissions,
        List<RoleAssignment> assignments) {

    public CurrentIdentitySnapshot {
        if (userId == null || subject == null || subject.isBlank() || permissions == null || assignments == null) {
            throw new IllegalArgumentException("current identity snapshot is incomplete");
        }
        permissions = List.copyOf(permissions);
        assignments = List.copyOf(assignments);
    }
}
