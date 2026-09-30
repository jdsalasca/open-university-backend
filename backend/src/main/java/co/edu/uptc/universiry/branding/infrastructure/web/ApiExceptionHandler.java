package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.branding.application.BrandingRevisionConflictException;
import co.edu.uptc.universiry.branding.application.BrandingRevisionNotFoundException;
import co.edu.uptc.universiry.branding.application.BrandingValidationException;
import co.edu.uptc.universiry.branding.application.BrandAssetNotFoundException;
import co.edu.uptc.universiry.branding.application.BrandAssetStorageException;
import co.edu.uptc.universiry.branding.application.BrandAssetTooLargeException;
import co.edu.uptc.universiry.branding.application.BrandAssetValidationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

    @ExceptionHandler(BrandAssetNotFoundException.class)
    ResponseEntity<ApiError> assetNotFound() {
        return error(HttpStatus.NOT_FOUND, "asset_not_found", "The published image asset does not exist.");
    }

    @ExceptionHandler({BrandAssetTooLargeException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<ApiError> assetTooLarge() {
        return error(HttpStatus.CONTENT_TOO_LARGE, "asset_too_large", "The image exceeds the configured upload limit.");
    }

    @ExceptionHandler(BrandAssetStorageException.class)
    ResponseEntity<ApiError> assetStorageFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "asset_storage_unavailable", "The image could not be stored or retrieved.");
    }

    @ExceptionHandler({BrandingValidationException.class, BrandAssetValidationException.class,
            MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MultipartException.class,
            MissingServletRequestPartException.class})
    ResponseEntity<ApiError> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "validation_failed", "Review the branding values and try again.");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "The requested change could not be saved.");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }

    public record ApiError(String error, String message) {
    }
}
