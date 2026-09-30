package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCatalogActorException;
import co.edu.uptc.universiry.academics.application.AcademicStructureConflictException;
import co.edu.uptc.universiry.academics.application.AcademicStructureNotFoundException;
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

@RestControllerAdvice(assignableTypes = AcademicStructureController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcademicStructureExceptionHandler {

    private final MessageCatalog messages;

    public AcademicStructureExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AcademicStructureNotFoundException.class)
    ResponseEntity<AcademicStructureErrorResponse> notFound() {
        return error(HttpStatus.NOT_FOUND, "academic_structure_not_found", "academic-structure.error.not-found");
    }

    @ExceptionHandler({AcademicStructureConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<AcademicStructureErrorResponse> conflict() {
        return error(HttpStatus.CONFLICT, "academic_structure_conflict", "academic-structure.error.conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<AcademicStructureErrorResponse> invalid() {
        return error(HttpStatus.BAD_REQUEST, "invalid_academic_structure", "academic-structure.error.invalid");
    }

    @ExceptionHandler(AcademicCatalogActorException.class)
    ResponseEntity<AcademicStructureErrorResponse> invalidActor() {
        return error(HttpStatus.FORBIDDEN, "invalid_actor", "academic-structure.error.actor");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<AcademicStructureErrorResponse> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "academic_structure_internal_error",
                "academic-structure.error.internal");
    }

    private ResponseEntity<AcademicStructureErrorResponse> error(HttpStatus status, String code, String key) {
        return ResponseEntity.status(status).body(new AcademicStructureErrorResponse(
                code, messages.message(key, org.springframework.context.i18n.LocaleContextHolder.getLocale())));
    }
}
