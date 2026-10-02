package co.edu.uptc.universiry.security.localpreview;

public final class LocalPreviewSessionLimitException extends RuntimeException {
    public LocalPreviewSessionLimitException() {
        super("The local preview session limit has been reached.");
    }
}
