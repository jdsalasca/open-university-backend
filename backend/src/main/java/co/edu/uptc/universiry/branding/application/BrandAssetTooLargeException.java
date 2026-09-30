package co.edu.uptc.universiry.branding.application;

public class BrandAssetTooLargeException extends RuntimeException {

    public BrandAssetTooLargeException() {
        super("The image exceeds the configured upload size limit.");
    }
}
