package co.edu.uptc.universiry.identity.application;

import co.edu.uptc.universiry.identity.domain.RoleAssignment;

import java.util.List;

public record CurrentIdentitySnapshot(String subject, List<String> permissions, List<RoleAssignment> assignments) {

    public CurrentIdentitySnapshot {
        if (subject == null || subject.isBlank() || permissions == null || assignments == null) {
            throw new IllegalArgumentException("current identity snapshot is incomplete");
        }
        permissions = List.copyOf(permissions);
        assignments = List.copyOf(assignments);
    }
}
