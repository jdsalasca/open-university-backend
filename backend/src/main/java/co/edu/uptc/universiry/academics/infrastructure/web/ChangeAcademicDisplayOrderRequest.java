package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicDisplayOrderCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeAcademicDisplayOrderRequest(
        @NotNull @Min(0) Integer expectedDisplayOrder,
        @NotNull @Min(0) @Max(100_000) Integer displayOrder,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicDisplayOrderCommand toCommand() {
        return new AcademicDisplayOrderCommand(expectedDisplayOrder, displayOrder, sourceReference);
    }
}
