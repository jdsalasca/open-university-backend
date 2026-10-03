package co.edu.uptc.universiry.library.application;

/** The lending is refused by a domain rule, such as an exemplar that already has an open loan. */
public class LibraryLoanRejectedException extends RuntimeException {

    public LibraryLoanRejectedException(String reason) {
        super(reason);
    }
}