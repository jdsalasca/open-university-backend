package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicStructureRelationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAcademicStructureRelationRequest(
        @Min(0) Integer displayOrder,
        @NotNull LocalDate validFrom,
        LocalDate validThrough,
        @NotBlank @Size(max = 240) String sourceReference
) {
    public CreateAcademicStructureRelationRequest {
        displayOrder = displayOrder == null ? 0 : displayOrder;
    }

    AcademicStructureRelationCommand toCommand() {
        return new AcademicStructureRelationCommand(displayOrder, validFrom, validThrough, sourceReference);
    }
}
