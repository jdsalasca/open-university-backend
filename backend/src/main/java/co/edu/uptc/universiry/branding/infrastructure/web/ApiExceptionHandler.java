package co.edu.uptc.universiry.branding.infrastructure.web;

import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
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
import org.springframework.context.i18n.LocaleContextHolder;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final MessageCatalog messages;

    public ApiExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BrandingRevisionConflictException.class)
    ResponseEntity<ApiError> revisionConflict() {
        return error(HttpStatus.CONFLICT, "revision_conflict", "api.error.revision-conflict");
    }

    @ExceptionHandler(BrandingRevisionNotFoundException.class)
    ResponseEntity<ApiError> revisionNotFound() {
        return error(HttpStatus.NOT_FOUND, "revision_not_found", "api.error.revision-not-found");
    }

    @ExceptionHandler(BrandAssetNotFoundException.class)
    ResponseEntity<ApiError> assetNotFound() {
        return error(HttpStatus.NOT_FOUND, "asset_not_found", "api.error.asset-not-found");
    }

    @ExceptionHandler({BrandAssetTooLargeException.class, MaxUploadSizeExceededException.class})
    ResponseEntity<ApiError> assetTooLarge() {
        return error(HttpStatus.CONTENT_TOO_LARGE, "asset_too_large", "api.error.asset-too-large");
    }

    @ExceptionHandler(BrandAssetStorageException.class)
    ResponseEntity<ApiError> assetStorageFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "asset_storage_unavailable", "api.error.asset-storage-unavailable");
    }

    @ExceptionHandler({BrandingValidationException.class, BrandAssetValidationException.class,
            MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MultipartException.class,
            MissingServletRequestPartException.class})
    ResponseEntity<ApiError> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "validation_failed", "api.error.validation-failed");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> persistenceFailure() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "api.error.internal-error");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String messageKey) {
        return ResponseEntity.status(status).body(new ApiError(
                code,
                messages.message(messageKey, LocaleContextHolder.getLocale())
        ));
    }

    public record ApiError(String error, String message) {
    }
}
