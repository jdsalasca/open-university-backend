package co.edu.uptc.universiry.identity.infrastructure.web;

public record RevokeRoleAssignmentRequest(long expectedVersion, String sourceReference) {
}
