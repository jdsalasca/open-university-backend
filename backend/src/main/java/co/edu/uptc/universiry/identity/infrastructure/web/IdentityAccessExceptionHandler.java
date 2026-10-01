package co.edu.uptc.universiry.identity.infrastructure.web;

import co.edu.uptc.universiry.identity.application.IdentityNotRegisteredException;
import co.edu.uptc.universiry.identity.application.RoleAssignmentNotFoundException;
import co.edu.uptc.universiry.identity.application.RoleAssignmentVersionConflictException;
import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice(assignableTypes = {RoleAssignmentController.class, CurrentIdentityController.class})
public class IdentityAccessExceptionHandler {

    private final MessageCatalog messages;

    public IdentityAccessExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<IdentityAccessErrorResponse> invalidRequest(
            IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "identity-access.invalid", request);
    }

    @ExceptionHandler(IdentityNotRegisteredException.class)
    ResponseEntity<IdentityAccessErrorResponse> identityNotRegistered(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "identity-access.identity-not-registered", request);
    }

    @ExceptionHandler(RoleAssignmentNotFoundException.class)
    ResponseEntity<IdentityAccessErrorResponse> assignmentNotFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "identity-access.assignment-not-found", request);
    }

    @ExceptionHandler(RoleAssignmentVersionConflictException.class)
    ResponseEntity<IdentityAccessErrorResponse> assignmentConflict(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "identity-access.assignment-conflict", request);
    }

    private ResponseEntity<IdentityAccessErrorResponse> error(
            HttpStatus status, String messageKey, HttpServletRequest request) {
        Locale locale = request.getHeader("Accept-Language") == null
                ? Locale.forLanguageTag("es-CO")
                : request.getLocale();
        return ResponseEntity.status(status)
                .body(new IdentityAccessErrorResponse(messageKey, messages.message(messageKey, locale)));
    }

    public record IdentityAccessErrorResponse(String error, String message) {
    }
}
