package co.edu.uptc.universiry.security;

public enum ApplicationPermission {
    BRANDING_READ("branding:read"),
    BRANDING_WRITE("branding:write"),
    ACADEMIC_CATALOG_READ("academic:catalog:read"),
    ACADEMIC_CATALOG_WRITE("academic:catalog:write");

    private final String authority;

    ApplicationPermission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
