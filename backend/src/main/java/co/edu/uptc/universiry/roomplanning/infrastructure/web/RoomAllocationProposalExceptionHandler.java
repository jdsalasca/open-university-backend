package co.edu.uptc.universiry.roomplanning.infrastructure.web;

import co.edu.uptc.universiry.roomplanning.application.RoomPlanningComplexityException;
import co.edu.uptc.universiry.roomplanning.domain.InvalidRoomPlanningScenarioException;
import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice(assignableTypes = RoomAllocationProposalController.class)
@Profile("local-preview")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RoomAllocationProposalExceptionHandler {

    private final MessageCatalog messages;

    public RoomAllocationProposalExceptionHandler(MessageCatalog messages) {
        this.messages = messages;
    }

    @ExceptionHandler({InvalidRoomPlanningScenarioException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<RoomAllocationProposalErrorResponse> invalidScenario() {
        return error(HttpStatus.BAD_REQUEST, "invalid_room_allocation", "room-allocation.error.invalid");
    }

    @ExceptionHandler(RoomPlanningComplexityException.class)
    ResponseEntity<RoomAllocationProposalErrorResponse> tooComplex() {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "room_allocation_too_complex", "room-allocation.error.too-complex");
    }

    private ResponseEntity<RoomAllocationProposalErrorResponse> error(HttpStatus status, String code, String messageKey) {
        Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale();
        return ResponseEntity.status(status).body(new RoomAllocationProposalErrorResponse(
                code, messages.message(messageKey, locale)));
    }
}
