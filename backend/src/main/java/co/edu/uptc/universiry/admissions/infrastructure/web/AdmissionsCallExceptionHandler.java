package co.edu.uptc.universiry.admissions.infrastructure.web;

import co.edu.uptc.universiry.admissions.application.AdmissionsActorException;
import co.edu.uptc.universiry.admissions.application.AdmissionsCallConflictException;
import co.edu.uptc.universiry.admissions.application.AdmissionsCallNotFoundException;
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

@RestControllerAdvice(assignableTypes = AdmissionsCallController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdmissionsCallExceptionHandler {

    private final MessageCatalog messages;

    public AdmissionsCallExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AdmissionsCallNotFoundException.class)
    ResponseEntity<AdmissionsCallErrorResponse> notFound() {
        return error(HttpStatus.NOT_FOUND, "admissions_call_not_found", "admissions.error.not-found");
    }

    @ExceptionHandler({AdmissionsCallConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<AdmissionsCallErrorResponse> conflict() {
        return error(HttpStatus.CONFLICT, "admissions_call_conflict", "admissions.error.conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<AdmissionsCallErrorResponse> invalid() {
        return error(HttpStatus.BAD_REQUEST, "invalid_admissions_call", "admissions.error.invalid");
    }

    @ExceptionHandler(AdmissionsActorException.class)
    ResponseEntity<AdmissionsCallErrorResponse> invalidActor() {
        return error(HttpStatus.FORBIDDEN, "invalid_admissions_actor", "admissions.error.actor");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<AdmissionsCallErrorResponse> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "admissions_call_internal_error", "admissions.error.internal");
    }

    private ResponseEntity<AdmissionsCallErrorResponse> error(HttpStatus status, String code, String key) {
        Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        return ResponseEntity.status(status).body(new AdmissionsCallErrorResponse(code, messages.message(key, locale)));
    }

    record AdmissionsCallErrorResponse(String error, String message) {
    }
}
