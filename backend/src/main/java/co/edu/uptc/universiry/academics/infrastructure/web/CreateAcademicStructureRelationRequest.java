package co.edu.uptc.universiry.academics.infrastructure.web;

import co.edu.uptc.universiry.academics.application.AcademicStructureRelationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAcademicStructureRelationRequest(
        @NotNull LocalDate validFrom,
        LocalDate validThrough,
        @NotBlank @Size(max = 240) String sourceReference
) {
    AcademicStructureRelationCommand toCommand() {
        return new AcademicStructureRelationCommand(validFrom, validThrough, sourceReference);
    }
}
