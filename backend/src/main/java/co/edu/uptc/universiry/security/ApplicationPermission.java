package co.edu.uptc.universiry.security;

public enum ApplicationPermission {
    BRANDING_READ("branding:read"),
    BRANDING_WRITE("branding:write"),
    ACADEMIC_CATALOG_READ("academic:catalog:read"),
    ACADEMIC_CATALOG_WRITE("academic:catalog:write"),
    ACADEMIC_STRUCTURE_READ("academic:structure:read"),
    ACADEMIC_STRUCTURE_WRITE("academic:structure:write"),
    ACADEMIC_PERIOD_READ("academic:period:read"),
    ACADEMIC_PERIOD_WRITE("academic:period:write"),
    IDENTITY_ROLES_READ("identity:roles:read"),
    IDENTITY_ROLES_WRITE("identity:roles:write");

    private final String authority;

    ApplicationPermission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
