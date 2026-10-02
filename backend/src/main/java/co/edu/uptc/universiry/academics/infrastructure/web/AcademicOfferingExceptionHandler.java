package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCatalogActorException;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftConflictException;
import co.edu.uptc.universiry.academics.application.AcademicOfferingDraftNotFoundException;
import co.edu.uptc.universiry.academics.application.AcademicOfferingReferenceNotFoundException;
import co.edu.uptc.universiry.academics.domain.AcademicOfferingVersionConflictException;
import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice(assignableTypes = AcademicOfferingDraftController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcademicOfferingExceptionHandler {

    private final MessageCatalog messages;

    public AcademicOfferingExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AcademicOfferingDraftNotFoundException.class)
    ResponseEntity<AcademicOfferingErrorResponse> notFound() {
        return error(HttpStatus.NOT_FOUND, "academic_offering_not_found", "academic-offering.error.not-found");
    }

    @ExceptionHandler(AcademicOfferingReferenceNotFoundException.class)
    ResponseEntity<AcademicOfferingErrorResponse> referenceNotFound() {
        return error(HttpStatus.NOT_FOUND, "academic_offering_reference_not_found",
                "academic-offering.error.reference-not-found");
    }

    @ExceptionHandler({AcademicOfferingDraftConflictException.class, AcademicOfferingVersionConflictException.class,
            DataIntegrityViolationException.class})
    ResponseEntity<AcademicOfferingErrorResponse> conflict() {
        return error(HttpStatus.CONFLICT, "academic_offering_conflict", "academic-offering.error.conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<AcademicOfferingErrorResponse> invalid() {
        return error(HttpStatus.BAD_REQUEST, "invalid_academic_offering", "academic-offering.error.invalid");
    }

    @ExceptionHandler(AcademicCatalogActorException.class)
    ResponseEntity<AcademicOfferingErrorResponse> invalidActor() {
        return error(HttpStatus.FORBIDDEN, "invalid_actor", "academic-offering.error.actor");
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    ResponseEntity<AcademicOfferingErrorResponse> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "academic_offering_internal_error",
                "academic-offering.error.internal");
    }

    private ResponseEntity<AcademicOfferingErrorResponse> error(HttpStatus status, String code, String messageKey) {
        Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        return ResponseEntity.status(status).body(new AcademicOfferingErrorResponse(
                code, messages.message(messageKey, locale)));
    }
}
