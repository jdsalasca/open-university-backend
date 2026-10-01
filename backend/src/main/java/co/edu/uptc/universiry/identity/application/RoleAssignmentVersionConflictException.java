package co.edu.uptc.universiry.identity.application;

public class RoleAssignmentVersionConflictException extends RuntimeException {

    public RoleAssignmentVersionConflictException() {
        super("Role assignment changed since it was read");
    }
}
