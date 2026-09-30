package co.edu.uptc.universiry.branding.application;

public class BrandingRevisionNotFoundException extends RuntimeException {

    public BrandingRevisionNotFoundException(long revision) {
        super("Branding revision " + revision + " was not found.");
    }
}
