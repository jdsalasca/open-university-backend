package co.edu.uptc.universiry.notices.infrastructure.web;

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

@RestControllerAdvice(assignableTypes = InstitutionalNoticeController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InstitutionalNoticeExceptionHandler {

    private final MessageCatalog messages;

    public InstitutionalNoticeExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<InstitutionalNoticeErrorResponse> invalid() {
        return error(HttpStatus.BAD_REQUEST, "invalid_institutional_notice", "notices.error.invalid");
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    ResponseEntity<InstitutionalNoticeErrorResponse> conflict() {
        return error(HttpStatus.CONFLICT, "institutional_notice_conflict", "notices.error.conflict");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<InstitutionalNoticeErrorResponse> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "institutional_notice_internal_error", "notices.error.internal");
    }

    private ResponseEntity<InstitutionalNoticeErrorResponse> error(HttpStatus status, String code, String key) {
        Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        return ResponseEntity.status(status).body(new InstitutionalNoticeErrorResponse(code, messages.message(key, locale)));
    }

    record InstitutionalNoticeErrorResponse(String error, String message) {
    }
}