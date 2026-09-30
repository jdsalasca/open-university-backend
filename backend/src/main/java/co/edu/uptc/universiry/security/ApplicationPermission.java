package co.edu.uptc.universiry.security;

public enum ApplicationPermission {
    BRANDING_READ("branding:read"),
    BRANDING_WRITE("branding:write");

    private final String authority;

    ApplicationPermission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
