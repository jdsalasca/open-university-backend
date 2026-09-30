package co.edu.uptc.universiry.branding.application;

public class BrandAssetNotFoundException extends RuntimeException {

    public BrandAssetNotFoundException() {
        super("The published image asset was not found.");
    }
}
