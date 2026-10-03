package co.edu.uptc.universiry.library.application;

/** The requested copy or loan does not exist. */
public class LibraryCopyNotFoundException extends RuntimeException {

    public LibraryCopyNotFoundException(String reference) {
        super("library record not found: " + reference);
    }
}