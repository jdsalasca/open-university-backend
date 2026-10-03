package co.edu.uptc.universiry.library.application;

/** The requested loan is not open, so it cannot be closed. */
public class LibraryLoanNotFoundException extends RuntimeException {

    public LibraryLoanNotFoundException(String loanId) {
        super("no open loan for reference: " + loanId);
    }
}