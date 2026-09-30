package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCatalogActorException;
import co.edu.uptc.universiry.academics.application.AcademicPeriodConflictException;
import co.edu.uptc.universiry.academics.application.AcademicPeriodNotFoundException;
import co.edu.uptc.universiry.academics.domain.AcademicPeriodStateConflictException;
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

@RestControllerAdvice(assignableTypes = AcademicPeriodController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcademicPeriodExceptionHandler {

    private final MessageCatalog messages;

    public AcademicPeriodExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AcademicPeriodNotFoundException.class)
    ResponseEntity<AcademicPeriodErrorResponse> notFound() {
        return error(HttpStatus.NOT_FOUND, "academic_period_not_found", "academic-period.error.not-found");
    }

    @ExceptionHandler({AcademicPeriodConflictException.class, AcademicPeriodStateConflictException.class,
            DataIntegrityViolationException.class})
    ResponseEntity<AcademicPeriodErrorResponse> conflict() {
        return error(HttpStatus.CONFLICT, "academic_period_conflict", "academic-period.error.conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<AcademicPeriodErrorResponse> invalid() {
        return error(HttpStatus.BAD_REQUEST, "invalid_academic_period", "academic-period.error.invalid");
    }

    @ExceptionHandler(AcademicCatalogActorException.class)
    ResponseEntity<AcademicPeriodErrorResponse> invalidActor() {
        return error(HttpStatus.FORBIDDEN, "invalid_actor", "academic-period.error.actor");
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    ResponseEntity<AcademicPeriodErrorResponse> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "academic_period_internal_error", "academic-period.error.internal");
    }

    private ResponseEntity<AcademicPeriodErrorResponse> error(HttpStatus status, String code, String messageKey) {
        Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        return ResponseEntity.status(status).body(new AcademicPeriodErrorResponse(
                code, messages.message(messageKey, locale)));
    }
}
