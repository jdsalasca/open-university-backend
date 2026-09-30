package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.branding.application.BrandingRevisionConflictException;
import co.edu.uptc.universiry.branding.application.BrandingRevisionNotFoundException;
import co.edu.uptc.universiry.branding.application.BrandingValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BrandingRevisionConflictException.class)
    ResponseEntity<ApiError> revisionConflict() {
        return error(HttpStatus.CONFLICT, "revision_conflict", "The branding configuration changed. Reload it before publishing.");
    }

    @ExceptionHandler(BrandingRevisionNotFoundException.class)
    ResponseEntity<ApiError> revisionNotFound() {
        return error(HttpStatus.NOT_FOUND, "revision_not_found", "The requested branding revision does not exist.");
    }

    @ExceptionHandler({BrandingValidationException.class, MethodArgumentNotValidException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "validation_failed", "Review the branding values and try again.");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }

    public record ApiError(String error, String message) {
    }
}
