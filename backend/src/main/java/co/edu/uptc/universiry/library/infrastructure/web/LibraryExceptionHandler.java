package co.edu.uptc.universiry.library.infrastructure.web;

import co.edu.uptc.universiry.library.application.LibraryCopyNotFoundException;
import co.edu.uptc.universiry.library.application.LibraryLoanNotFoundException;
import co.edu.uptc.universiry.library.application.LibraryLoanRejectedException;
import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice(assignableTypes = LibraryController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LibraryExceptionHandler {

    private final MessageCatalog messages;

    public LibraryExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler({LibraryCopyNotFoundException.class, LibraryLoanNotFoundException.class})
    ResponseEntity<LibraryErrorResponse> notFound() {
        return error(HttpStatus.NOT_FOUND, "library_record_not_found", "library.error.not-found");
    }

    /**
     * A rejected lending is a conflict with the current state of the copy: it already has an open loan, or it was
     * withdrawn from circulation. A foreign key on a borrower that does not exist is an invalid request instead.
     */
    @ExceptionHandler({LibraryLoanRejectedException.class})
    ResponseEntity<LibraryErrorResponse> rejected() {
        return error(HttpStatus.CONFLICT, "library_loan_rejected", "library.error.rejected");
    }

    @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
    ResponseEntity<LibraryErrorResponse> conflict() {
        return error(HttpStatus.CONFLICT, "library_conflict", "library.error.conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<LibraryErrorResponse> invalid() {
        return error(HttpStatus.BAD_REQUEST, "invalid_library_request", "library.error.invalid");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<LibraryErrorResponse> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "library_internal_error", "library.error.internal");
    }

    private ResponseEntity<LibraryErrorResponse> error(HttpStatus status, String code, String key) {
        Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        return ResponseEntity.status(status).body(new LibraryErrorResponse(code, messages.message(key, locale)));
    }

    record LibraryErrorResponse(String error, String message) {
    }
}