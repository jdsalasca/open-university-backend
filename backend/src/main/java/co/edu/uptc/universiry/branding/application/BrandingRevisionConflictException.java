package co.edu.uptc.universiry.branding.application;

public class BrandingRevisionConflictException extends RuntimeException {

    public BrandingRevisionConflictException() {
        super("The branding configuration changed after this draft was opened. Reload and try again.");
    }
}
