package co.edu.uptc.universiry.identity.application;

public class RoleAssignmentNotFoundException extends RuntimeException {

    public RoleAssignmentNotFoundException() {
        super("Role assignment was not found");
    }
}
