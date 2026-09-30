package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicCatalogNotFoundException;
import co.edu.uptc.universiry.academics.application.AcademicCatalogActorException;
import co.edu.uptc.universiry.academics.application.InvalidCurriculumEntriesPageQueryException;
import co.edu.uptc.universiry.academics.application.InvalidCurriculumDraftsPageQueryException;
import co.edu.uptc.universiry.academics.application.CurriculumCsvException;
import co.edu.uptc.universiry.academics.application.CurriculumImportSourceException;
import co.edu.uptc.universiry.academics.application.CurriculumVersionConflictException;
import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;

@RestControllerAdvice(assignableTypes = AcademicCatalogController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcademicCatalogExceptionHandler {

    private final MessageCatalog messages;

    public AcademicCatalogExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AcademicCatalogNotFoundException.class)
    ResponseEntity<AcademicCatalogErrorResponse> notFound() {
        return error(HttpStatus.NOT_FOUND, "curriculum_not_found", "academic-catalog.error.not-found");
    }

    @ExceptionHandler(InvalidCurriculumEntriesPageQueryException.class)
    ResponseEntity<AcademicCatalogErrorResponse> invalidEntriesQuery() {
        return error(HttpStatus.BAD_REQUEST, "invalid_curriculum_entries_query",
                "academic-catalog.error.invalid-entries-query");
    }

    @ExceptionHandler(InvalidCurriculumDraftsPageQueryException.class)
    ResponseEntity<AcademicCatalogErrorResponse> invalidDraftsQuery() {
        return error(HttpStatus.BAD_REQUEST, "invalid_curriculum_drafts_page_query",
                "academic-catalog.error.invalid-drafts-query");
    }

    @ExceptionHandler(CurriculumVersionConflictException.class)
    ResponseEntity<AcademicCatalogErrorResponse> conflict() {
        return error(HttpStatus.CONFLICT, "curriculum_conflict", "academic-catalog.error.conflict");
    }

    @ExceptionHandler(AcademicCatalogActorException.class)
    ResponseEntity<AcademicCatalogErrorResponse> invalidActor() {
        return error(HttpStatus.FORBIDDEN, "invalid_actor", "academic-catalog.error.actor");
    }

    @ExceptionHandler(CurriculumCsvException.class)
    ResponseEntity<AcademicCatalogErrorResponse> invalidCsv(CurriculumCsvException exception) {
        boolean tooLarge = exception.code() == CurriculumCsvException.Code.FILE_TOO_LARGE
                || exception.code() == CurriculumCsvException.Code.TOO_MANY_ROWS;
        HttpStatus status = tooLarge ? HttpStatus.CONTENT_TOO_LARGE : HttpStatus.BAD_REQUEST;
        String code = tooLarge ? "curriculum_too_large" : "invalid_curriculum_csv";
        String key = tooLarge ? "academic-catalog.error.too-large" : "academic-catalog.error.invalid-csv";
        List<AcademicCatalogIssueResponse> issues = exception.issues().stream()
                .map(AcademicCatalogIssueResponse::from)
                .toList();
        return ResponseEntity.status(status).body(new AcademicCatalogErrorResponse(
                code, messages.message(key, LocaleContextHolder.getLocale()), issues));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<AcademicCatalogErrorResponse> uploadTooLarge() {
        return error(HttpStatus.CONTENT_TOO_LARGE, "curriculum_too_large", "academic-catalog.error.too-large");
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    ResponseEntity<AcademicCatalogErrorResponse> malformedUpload() {
        return error(HttpStatus.BAD_REQUEST, "invalid_curriculum_csv", "academic-catalog.error.invalid-csv");
    }

    @ExceptionHandler({CurriculumImportSourceException.class, DataAccessException.class})
    ResponseEntity<AcademicCatalogErrorResponse> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "academic-catalog.error.internal");
    }

    private ResponseEntity<AcademicCatalogErrorResponse> error(
            HttpStatus status,
            String code,
            String messageKey
    ) {
        return ResponseEntity.status(status).body(AcademicCatalogErrorResponse.of(
                code, messages.message(messageKey, LocaleContextHolder.getLocale())));
    }
}
